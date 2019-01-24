package com.github.dsebban.http4s.crud.routes.mongo

import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import cats.effect.IO
import cats.effect.LiftIO
import types._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.commands.bson.DefaultBSONCommandError
// import reactivemongo.api.indexes.{ Index, IndexType }
import reactivemongo.api.indexes.{  IndexType }

import reactivemongo.api.{ Cursor,  ReadPreference }
import reactivemongo.bson.{ BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID }
import reactivemongo.core.errors.DatabaseException

import scala.concurrent.Future
import scala.util.Try

object MongoErrorCodes {
  val DuplicateUniqueKeyErrorCode = 11000
}

object MongoHelpers {
  sealed trait Order
  case object Ascending extends Order
  case object Descending extends Order

  case class IndexInformation(key: String, unique: Boolean, indexType: IndexType)
  case class FieldName(name: String) extends AnyVal
  case class SortBy(fieldName: FieldName, order: Order) {
    val orderInt: Int = order match {
      case Ascending  => 1
      case Descending => -1
    }
    val bson: BSONDocument = BSONDocument(fieldName.name -> orderInt)

  }

  import MongoErrorCodes._

  trait MongoError extends Exception

  case class InvalidObjectId(objId: String) extends Exception(s"Invalid Mongo object Id: ${objId}") with MongoError
  case class ObjectNotFound(objId: String)
      extends NoSuchElementException(s"Not found, object with Id: ${objId}")
      with MongoError
  case class ObjectWithPropertyNotFound(propName: String, propValue: String)
      extends NoSuchElementException(s"Nof Found object with ${propName}: ${propValue}")
      with MongoError
  case class DuplicateUniqueKeyError(message: String) extends Exception(message) with MongoError

  object MongoError {
    import com.github.dsebban.http4s.crud.routes.core._
    import org.http4s.dsl.Http4sDsl
    import cats.MonadError
    import io.circe.syntax._
    import org.http4s.circe._
    import org.http4s._

    implicit def mongoErrorHandler[F[_]: MonadError[?[_], MongoError]]: HttpErrorHandler[F, MongoError] =
      new MongoErrorHandler[F]

    class MongoErrorHandler[F[_]: MonadError[?[_], MongoError]]
        extends HttpErrorHandler[F, MongoError]
        with Http4sDsl[F] {
      private val handler: MongoError => F[Response[F]] = {
        case InvalidObjectId(id)                       => BadRequest(s"Invalid $id is ".asJson)
        case ObjectNotFound(id)                             => BadRequest(s"Invalid id $id".asJson)
        case DuplicateUniqueKeyError(id)               => Conflict(s"id $id already exists!".asJson)
        case ObjectWithPropertyNotFound(propName, _) => NotFound(s"User not found: $propName".asJson)
      }
      override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
        RoutesHttpErrorHandler(routes)(handler)
    }
  }

  // This is deliberately sync to make sure collection is created
  // def initCollection(
  //     connection: MongoConnection,
  //     databaseName: String,
  //     collectionName: String,
  //     indexKey: Option[String]
  // ): Future[BSONCollection] =
  //   for {
  //     db <- connection.database(databaseName)
  //     col = db.collection(collectionName)
  //     _ <- indexKey.fold(Future.successful(false))(
  //           key => ensureIndex(col = col, IndexInformation(key, unique = true, IndexType.Ascending))
  //         )
  //   } yield col

  // // This is deliberately sync to endure index
  // def ensureIndex(
  //     col: BSONCollection,
  //     index: IndexInformation
  // ): Future[Boolean] =
  //   col.indexesManager
  //     .ensure(Index(Seq(index.key -> index.indexType), unique = index.unique, background = true))

  def lift[F[_]: LiftIO, A](future: Future[A])(errors: Throwable => IO[A])(implicit L: LiftIO[F]): F[A] =
    L.liftIO(IO.fromFuture(IO(future)).handleErrorWith(errors))

  def create[F[_]: LiftIO, O](
      mongoCollection: BSONCollection,
      id: BSONObjectID,
      obj: O
  )(implicit
    ev: BSONDocumentWriter[WithId[O]]): F[Unit] =
    lift(mongoCollection.insert(WithId(id.stringify, obj)).void) {
      case de: DatabaseException if de.code == Some(DuplicateUniqueKeyErrorCode) =>
        IO.raiseError(DuplicateUniqueKeyError(de.message))
      case others => IO.raiseError(others)
    }

  def update[F[_], U, O](
      mongoCollection: BSONCollection,
      id: BSONObjectID,
      updateData: U
  )(implicit
    F: LiftIO[F],
    wr: BSONDocumentWriter[U],
    rd: BSONDocumentReader[WithId[U]]): F[Option[U]] =
    lift(
      mongoCollection
        .findAndUpdate(
          BSONDocument("_id" -> id),
          BSONDocument("$set" -> updateData),
          fetchNewObject = true
        )
        .map(_.result[WithId[U]])
        .map(_.map(_.data))
    ) {
      case ce: DefaultBSONCommandError if ce.code == Some(DuplicateUniqueKeyErrorCode) =>
        IO.raiseError(DuplicateUniqueKeyError(ce.errmsg.getOrElse("")))
      case others => IO.raiseError(others)
    }

  def upsertByProperty[U, O](
      mongoCollection: BSONCollection,
      updateData: U,
      name: String,
      value: String
  )(implicit
    wr: BSONDocumentWriter[U],
    rd: BSONDocumentReader[WithId[O]]): Future[Option[WithId[O]]] =
    mongoCollection
      .findAndUpdate(BSONDocument(name -> value), BSONDocument("$set" -> updateData), upsert = true)
      .map(_.result[WithId[O]])

  def getById[O](
      mongoCollection: BSONCollection,
      objId: String
  )(implicit
    ev: BSONDocumentReader[WithId[O]]): Future[Option[WithId[O]]] = {
    val parsedId: Try[BSONObjectID] = BSONObjectID.parse(objId)
    if (parsedId.isFailure) {
      Future.failed(InvalidObjectId(objId))
    } else {
      val selector = BSONDocument("_id" -> parsedId.get)
      mongoCollection
        .find(selector, None)
        .cursor[WithId[O]](ReadPreference.Primary)
        .headOption
    }
  }

  def getByProperty[O](
      mongoCollection: BSONCollection,
      name: String,
      value: String
  )(implicit
    ev: BSONDocumentReader[WithId[O]]): Future[Option[WithId[O]]] = {

    val selector = BSONDocument(name -> value)
    mongoCollection
      .find(selector, None)
      .cursor[WithId[O]](ReadPreference.Primary)
      .headOption
  }

  def getAll[O](
      mongoCollection: BSONCollection,
      findFilter: BSONDocument = BSONDocument(),
      limit: Option[Integer] = None,
      sortBy: Option[SortBy] = None
  )(implicit
    ev: BSONDocumentReader[WithId[O]]): Future[List[WithId[O]]] = {
    val maxDocs: Integer = limit match {
      case Some(n) => n
      case None    => Int.MaxValue
    }

    mongoCollection
      .find(findFilter, None)
      .sort(sortBy.map(_.bson).getOrElse(BSONDocument()))
      .cursor[WithId[O]](ReadPreference.Primary)
      .collect[List](maxDocs, Cursor.FailOnError())
  }

  def removeAll(
      mongoCollection: BSONCollection
  ): Future[WriteResult] =
    mongoCollection.delete().one(BSONDocument())

  def remove(
      mongoCollection: BSONCollection,
      objId: String
  ): Future[Unit] = {
    val parsedId = BSONObjectID.parse(objId)
    if (parsedId.isFailure) {
      Future.failed(InvalidObjectId(objId))
    } else {
      mongoCollection
        .delete()
        .one(BSONDocument("_id" -> parsedId.get))
        .flatMap { wr =>
          if (wr.n > 0)
            Future.successful(())
          else
            Future.failed(ObjectNotFound(objId))
        }
    }
  }

  def removeByProperty(
      mongoCollection: BSONCollection,
      name: String,
      value: String
  ): Future[Unit] =
    mongoCollection
      .delete()
      .one(BSONDocument(name -> value))
      .flatMap { wr =>
        if (wr.n > 0)
          Future.successful(())
        else
          Future.failed(ObjectWithPropertyNotFound(name, value))
      }
}

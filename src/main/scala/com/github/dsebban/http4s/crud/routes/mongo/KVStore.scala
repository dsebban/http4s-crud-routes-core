package com.github.dsebban.http4s.crud.routes.mongo

import fs2.Stream

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._

trait KVStore[F[_], K, V, A] {
  def get(k: K, a: A): F[Option[V]]
  def put(k: K, v: V, a: A): F[Unit]
  def delete(k: K, a: A): F[Unit]
  def scan(a: A): Stream[F, (K, V)]
}

object KVStore {
  def createInMemory[F[_], V, A](
      implicit F: Sync[F]
  ): F[KVStore[F, String, V, A]] =
    Ref.of[F, Map[String, V]](Map.empty).map { state =>
      new KVStore[F, String, V, A] {
        def get(k: String, a: A): F[Option[V]]  = state.get.map(_.get(k))
        def put(k: String, v: V, a: A): F[Unit] = state.update(_ + (k -> v)) *> F.unit
        def delete(k: String, a: A): F[Unit]    = state.update(_ - k) *> F.unit
        def scan(a: A): Stream[F, (String, V)] =
          Stream.eval(state.get.map(v => Stream.fromIterator(v.toList.iterator))).flatten
      }
    }
  import reactivemongo.api.{ DefaultDB, ReadPreference }
  import reactivemongo.bson.{ BSONDocument, BSONDocumentReader, BSONObjectID }
  import scala.concurrent.ExecutionContext.Implicits.global
  import cats.effect.IO

  def create[V, A](dbF: IO[DefaultDB], colName: A => String)(
      implicit reader: BSONDocumentReader[V]
  ): IO[KVStore[IO, String, V, A]] = {
    def validateId(id: String): IO[BSONObjectID] = ???
    dbF.map { db =>
      new KVStore[IO, String, V, A] {
        def get(k: String, a: A): IO[Option[V]] =
          validateId(k) >>=
            (
                bsonId =>
                  IO.fromFuture(IO {
                    db.collection(colName(a))
                      .find(BSONDocument("_id" -> bsonId), None)
                      .cursor[V](ReadPreference.Primary)
                      .headOption
                  })
            )

        def put(k: String, v: V, a: A): IO[Unit] = ???
        def delete(k: String, a: A): IO[Unit]    = ???
        def scan(a: A): Stream[IO, (String, V)] =
          ???
      }
    }
  }

  // def getMongoConnection[F[_]](mongoAddress: String, dbName: String)(implicit F: Sync[F]): F[MongoConnection] = F.sync {

  // }

}

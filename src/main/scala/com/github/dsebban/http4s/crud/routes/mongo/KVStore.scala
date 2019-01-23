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
  import reactivemongo.bson.{ BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID }
  import scala.concurrent.ExecutionContext.Implicits.global
  import cats.effect.IO
  import cats.effect.LiftIO

  def create[F[_], V, A](dbF: F[DefaultDB], colName: A => String)(
      implicit F: Sync[F],
      L: LiftIO[F],
      reader: BSONDocumentReader[V],
      writer: BSONDocumentWriter[V]
  ): F[KVStore[F, String, V, A]] = {
    def validateId(id: String): F[BSONObjectID] = BSONObjectID.parse(id).fold(F.raiseError, F.pure)
    dbF.map { db =>
      new KVStore[F, String, V, A] {
        def get(k: String, a: A): F[Option[V]] =
          validateId(k) >>=
            (
                bsonId =>
                  L.liftIO {
                    IO.fromFuture(IO {
                      db.collection(colName(a))
                        .find(BSONDocument("_id" -> bsonId), None)
                        .cursor[V](ReadPreference.Primary)
                        .headOption
                    })
                  }
            )

        def put(k: String, v: V, a: A): F[Unit] =
          validateId(k) >>=
            (
                _ =>
                  L.liftIO {
                    IO.fromFuture(IO {
                      db.collection(colName(a))
                        .insert(v)
                    })
                  } *> F.unit
            )
        def delete(k: String, a: A): F[Unit] = ???
        def scan(a: A): Stream[F, (String, V)] =
          ???
      }
    }
  }

  // def getMongoConnection[F[_]](mongoAddress: String, dbName: String)(implicit F: Sync[F]): F[MongoConnection] = F.sync {

  // }

}

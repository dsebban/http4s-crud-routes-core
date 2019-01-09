package com.github.dsebban.http4s.crud.routes.mongo

import com.github.dsebban.http4s.crud.routes.core.algebra._
import fs2.Stream
import cats.effect.Sync

import cats.syntax.all._

object interpreter {
  def toResourceAlgebra[F[_], V](kvStore: KVStore[F, String, V],
                                 keyGen: V => String)(implicit F: Sync[F]): ResourceAlgebra[F, V] =
    new ResourceAlgebra[F, V] {
      def save(r: V): F[(String, V)] = {
        val key = keyGen(r)
        kvStore.put(key, r) *> F.pure((key, r))
      }
      def find(id: String): F[Option[V]] = kvStore.get(id)
      def delete(id: String): F[Unit]    = kvStore.delete(id)
      def list: Stream[F, (String, V)]   = kvStore.scan
    }

  def toResourceAlgebraF[F[_], V](keyGen: V => String)(implicit F: Sync[F],
                                                       KV: F[KVStore[F, String, V]]): F[ResourceAlgebra[F, V]] =
    KV.map(kvStore => toResourceAlgebra(kvStore, keyGen))
}

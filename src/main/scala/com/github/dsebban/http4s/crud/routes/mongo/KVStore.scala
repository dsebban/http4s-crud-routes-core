package com.github.dsebban.http4s.crud.routes.mongo

import fs2.Stream

trait KVStore[F[_], K, V] {

  def get(k: K): F[Option[V]]
  def getAll(ks: List[K]): F[Map[K, V]]
  def put(k: K, v: V): F[Unit]
  def delete(k: K): F[Unit]
  def scan: Stream[F, (K, V)]
}

object KVStore {
  import cats.effect.Sync
  import cats.effect.concurrent.Ref
  import cats.syntax.all._
  def createInMemory[F[_], K, V](
      implicit F: Sync[F]
  ): F[KVStore[F, String, V]] =
    Ref.of[F, Map[String, V]](Map.empty).map { state =>
      new KVStore[F, String, V] {
        def get(k: String): F[Option[V]]                = state.get.map(_.get(k))
        def getAll(ks: List[String]): F[Map[String, V]] = state.get
        def put(k: String, v: V): F[Unit]               = state.get.map(_ + (k -> v)) *> F.unit
        def delete(k: String): F[Unit]                  = state.get.map(_ - k) *> F.unit
        def scan: Stream[F, (String, V)] =
          Stream.eval(state.get.map(v => Stream.fromIterator(v.toList.iterator))).flatten
      }
    }
}

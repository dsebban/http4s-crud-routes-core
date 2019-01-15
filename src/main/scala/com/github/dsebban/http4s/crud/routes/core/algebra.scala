package com.github.dsebban.http4s.crud.routes.core

import fs2.Stream

object algebra {

  trait ResourceAlgebra[F[_], R] {
    def save(r: R): F[(String, R)]
    def find(id: String): F[Option[R]]
    def delete(id: String): F[Unit]
    def list: Stream[F, (String, R)]
    // def update[P](id: String, field: R => P, newValue: P, copy: (R, P) => R): F[Unit]
  }

  trait AuthedResourceAlgebra[F[_], R, A] {
    def save(r: R, user: A): F[(String, R)]
    def find(id: String, user: A): F[Option[R]]
    def delete(id: String, user: A): F[Unit]
    def list(user: A): Stream[F, (String, R)]
    // def update[P](id: String, field: R => P, newValue: P, copy: (R, P) => R): F[Unit]
  }

}

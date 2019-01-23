package com.github.dsebban.http4s.crud.routes.core

import fs2.Stream

object algebra {

  type Id = String
  trait ResourceAlgebra[F[_], R] {
    def save(r: R): F[(Id, R)]
    def find(id: Id): F[Option[R]]
    def delete(id: Id): F[Unit]
    def list: Stream[F, (Id, R)]
    // def update[P](id: String, field: R => P, newValue: P, copy: (R, P) => R): F[Unit]
  }

  //define this in term of ResourceAlgebra
  trait AuthedResourceAlgebra[F[_], R, A] {
    def save(r: R, user: A): F[(Id, R)]
    def find(id: Id, user: A): F[Option[R]]
    def delete(id: Id, user: A): F[Unit]
    def list(user: A): Stream[F, (Id, R)]
    // def update[P](id: String, field: R => P, newValue: P, copy: (R, P) => R): F[Unit]
  }

}

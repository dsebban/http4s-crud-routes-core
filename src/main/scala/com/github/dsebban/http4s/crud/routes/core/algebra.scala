package com.github.dsebban.http4s.crud.routes.core

import fs2.Stream

object algebra {

  trait ResourceAlgebra[F[_], R] {
    def list: F[Stream[F, R]]
    def find(id: String): F[Option[R]]
    def save(user: R): F[R]
    def update[P](id: String, field: R => P, newValue: P, copy: (R, P) => R): F[Unit]
  }

}

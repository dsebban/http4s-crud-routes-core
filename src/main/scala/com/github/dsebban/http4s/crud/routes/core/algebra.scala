package com.github.dsebban.http4s.crud.routes.core

import domain._

object algebra {

  trait UserAlg[F[_]] {
    def find(username: String): F[Option[User]]
    def save(user: User): F[Unit]
    def updateAge(username: String, age: Int): F[Unit]
  }

}

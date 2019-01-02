package com.github.dsebban.http4s.crud.routes.core

import cats.data.EitherNel
import cats.implicits._

object domain {

  case class User(id: Option[String], username: String, age: Int)
  case class UserUpdateAge(age: Int)

  sealed trait UserError extends Exception
  object UserError {
    case class UserAlreadyExists(username: String) extends UserError
    case class UserNotFound(username: String) extends UserError
    case class NameTooLong(username: String) extends UserError
    case class InvalidUserAge(age: Int) extends UserError

    def validate(user: User): EitherNel[UserError, User] = {
      val validateName =
        user.username
          .asRight[UserError]
          .ensure(NameTooLong(user.username))(_.length <= 20)
          .toEitherNel

      val validateAge =
        user.age.asRight[UserError].ensure(InvalidUserAge(user.age))(_ > 0).toEitherNel

      (validateName, validateAge)
        .parMapN(User(user.id, _, _))
    }

  }

}

package com.github.dsebban.http4s.crud.routes.core

import cats.data.EitherNel
import cats.implicits._

import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import cats.MonadError
import org.http4s._

object domain {

  case class AuthInfo(organization: String, user: String)
  case class User(username: String, age: Int)
  case class UserUpdateAge(age: Int)

  sealed trait UserError extends Exception
  object UserError {
    case class UserAlreadyExists(user: User) extends UserError
    case class UserNotFound(username: String) extends UserError
    case class NameTooLong(username: String) extends UserError
    case class InvalidUserAge(age: Int) extends UserError

    def validate: User => EitherNel[UserError, User] = { user =>
      val validateName =
        user.username
          .asRight[UserError]
          .ensure(NameTooLong(user.username))(_.length <= 20)
          .toEitherNel

      val validateAge =
        user.age.asRight[UserError].ensure(InvalidUserAge(user.age))(_ > 0).toEitherNel

      (validateName, validateAge)
        .parMapN(User(_, _))
    }

    implicit def errorHandler[F[_]: MonadError[?[_], UserError]]: HttpErrorHandler[F, UserError] =
      new UserHttpErrorHandler[F]

    class UserHttpErrorHandler[F[_]: MonadError[?[_], UserError]]
        extends HttpErrorHandler[F, UserError]
        with Http4sDsl[F] {
      private val handler: UserError => F[Response[F]] = {
        case NameTooLong(username)       => BadRequest(s"Name $username is too long".asJson)
        case InvalidUserAge(age)         => BadRequest(s"Invalid age $age".asJson)
        case UserAlreadyExists(username) => Conflict(s"Username $username already exists!".asJson)
        case UserNotFound(username)      => NotFound(s"User not found: $username".asJson)
      }

      override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
        RoutesHttpErrorHandler(routes)(handler)
    }
  }

}

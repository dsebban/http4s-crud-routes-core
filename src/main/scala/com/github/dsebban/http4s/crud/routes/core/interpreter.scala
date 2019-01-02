package com.github.dsebban.http4s.crud.routes.core

import algebra._
import domain._
import UserError._
import cats.effect.Sync
import cats.effect.concurrent.Ref
import fs2.Stream
import cats.syntax.all._

object interpreter {

  def create[F[_]](implicit F: Sync[F]): F[ResourceAlgebra[F, User]] =
    Ref.of[F, Map[String, User]](Map.empty).map { state =>
      new ResourceAlgebra[F, User] {
        private def validate(user: User): F[User] =
          UserError.validate(user).fold(error => F.raiseError[User](error.head), (user => F.pure(user)))

        override def list: F[Stream[F, User]] =
          state.get.map(v => Stream.fromIterator(v.values.iterator).covary[F])

        override def find(id: String): F[Option[User]] =
          state.get.map(_.get(id))

        override def save(user: User): F[User] = {

          val createIfNotExists: Option[User] => F[User] = { maybeUser =>
            val newUser = user.copy(id = Some(java.util.UUID.randomUUID.toString))
            maybeUser.fold {
              state.update(_.updated(newUser.id.getOrElse(""), newUser))
            }(u => F.raiseError(UserAlreadyExists(u.username))) *> F.pure(newUser)

          }

          validate(user) *> find(user.id.getOrElse("")) >>= createIfNotExists

        }

        override def update[P](id: String, field: User => P, newValue: P, copy: (User, P) => User): F[Unit] = {
          def validate: Option[User] => F[User] =
            _.fold(F.raiseError[User](UserNotFound(id)))(
              user => UserError.validate(user).fold(error => F.raiseError[User](error.head), (user => F.pure(user)))
            )

          val updateIfExists: User => F[Unit] =
            user => state.update(_.updated(id, user))
          //   state.update(
          //     _.updated(id, (p => (r: User) => copy(r, p)).modify(_ => newValue)(u))
          // )

          //TODO: validate based on Lens ???
          find(id) >>= validate >>= updateIfExists

        }
      }
    }

}

package com.github.dsebban.http4s.crud.routes.core

import algebra._
import cats.data.EitherNel
import cats.effect.Sync
import cats.effect.concurrent.Ref
import fs2.Stream
import cats.syntax.all._

object interpreter {
  case class AlreadyExists[R](r: R) extends Exception

  def create[F[_], R, E <: Throwable](validate: R => EitherNel[E, R], generateId: R => String)(
      implicit F: Sync[F]
  ): F[ResourceAlgebra[F, R]] =
    Ref.of[F, Map[String, R]](Map.empty).map { state =>
      new ResourceAlgebra[F, R] {

        override def list: F[Stream[F, (String, R)]] =
          state.get.map(v => Stream.fromIterator(v.toList.iterator).covary[F])

        override def find(id: String): F[Option[R]] =
          state.get.map(_.get(id))

        override def save(user: R): F[(String, R)] = {
          val id = generateId(user)

          def validateF(user: R): F[R] =
            validate(user).fold(error => F.raiseError[R](error.head), (user => F.pure(user)))

          val createIfNotExists: Option[R] => F[(String, R)] = { maybeUser =>
            maybeUser.fold {
              state.update(_.updated(id, user))
            }(u => F.raiseError(AlreadyExists(u))) *> F.pure((id, user))

          }

          validateF(user) *> find(id) >>= createIfNotExists

        }

        // override def update[P](id: String, field: User => P, newValue: P, copy: (User, P) => User): F[Unit] = {
        //   def validate: Option[R] => F[R] =
        //     _.fold(F.raiseError[R](UserNotFound(id)))(
        //       user => UserError.validate(user).fold(error => F.raiseError[R](error.head), (user => F.pure(user)))
        //     )

        //   val updateIfExists: User => F[Unit] =
        //     user => state.update(_.updated(id, user))
        //   //   state.update(
        //   //     _.updated(id, (p => (r: R) => copy(r, p)).modify(_ => newValue)(u))
        //   // )

        //   //TODO: validate based on Lens ???
        //   find(id) >>= validate >>= updateIfExists

        // }
      }
    }

}

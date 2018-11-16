package com.github.dsebban.http4s.crud.routes.core

import algebra._
import cats.effect.Sync

import cats.data.EitherNel
import cats.syntax.all._
import io.circe.syntax._
import io.circe._
import org.http4s._
import org.http4s.server.AuthMiddleware
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._

import org.http4s.dsl.Http4sDsl

class UserRoutesMTL[F[_], R, E <: Throwable](resourceAlgebra: ResourceAlgebra[F, R])(implicit F: Sync[F],
                                                                                     E: Encoder[R],
                                                                                     D: Decoder[R],
                                                                                     H: HttpErrorHandler[F, E])
    extends Http4sDsl[F] {

  def getAll: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root =>
        Ok(resourceAlgebra.list.map(_.asJson))
    }

  def getByID: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / id =>
        resourceAlgebra.find(id) >>= (_.fold(NotFound(s"Not found: $id".asJson))(r => Ok(r.asJson)))
    }

  def create: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root =>
        req.as[R] >>= (r => resourceAlgebra.save(r)) >>= { case (id, _) => Created(id.asJson) }
    }

  def delete: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case DELETE -> Root / id =>
        resourceAlgebra.delete(id) *> Ok()
    }

  val routes: HttpRoutes[F] = H.handle(getAll <+> getByID <+> create <+> delete)

}

class UserAuthedRoutesMTL[F[_], R, E <: Throwable, ME <: Throwable, A](
    resourceAlgebra: AuthedResourceAlgebra[F, R, A],
    Middleware: AuthMiddleware[F, A],
    validator: (R, A) => EitherNel[E, R]
)(
    implicit F: Sync[F],
    E: Encoder[R],
    D: Decoder[R],
    H: HttpErrorHandler[F, E]
) extends Http4sDsl[F] {

  def validateF(resource: R, auth: A): F[R] =
    validator(resource, auth)
      .fold(errors => F.raiseError[R](errors.head), (resource => F.pure(resource)))

  def getAll: AuthedService[A, F] =
    AuthedService[A, F] {
      case GET -> Root as auth =>
        Ok(resourceAlgebra.list(auth).map(_.asJson))
    }

  def getByID: AuthedService[A, F] =
    AuthedService[A, F] {
      case GET -> Root / id as auth =>
        resourceAlgebra.find(id, auth) >>= (_.fold(NotFound(s"Not found: $id".asJson))(r => Ok(r.asJson)))
    }

  def create: AuthedService[A, F] =
    AuthedService[A, F] {
      case req @ POST -> Root as auth =>
        req.req.as[R] >>= (r => validateF(r, auth)) >>= (r => resourceAlgebra.save(r, auth)) >>= {
          case (id, _) => Created(id.asJson)
        }
    }

  def delete: AuthedService[A, F] =
    AuthedService[A, F] {
      case DELETE -> Root / id as auth =>
        resourceAlgebra.delete(id, auth) *> Ok()
    }

// def update: HttpRoutes[F] =
//     H.handle {
//       HttpRoutes.of[F] {
//     case req @ PUT -> Root / id =>
//       req.as[UserUpdateAge] >>= (
//           uu =>
//             resourceAlgebra.update(id, _.age, uu.age, (u: User, newAge: Int) => u.copy(age = newAge)) *> Created(
//               id.asJson
//             )
//       )
//   }

  val routes: HttpRoutes[F] = H.handle(Middleware(getAll <+> getByID <+> create <+> delete))

}

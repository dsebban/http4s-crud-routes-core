package com.github.dsebban.http4s.crud.routes.core

import algebra._
import cats.effect.Sync
import cats.syntax.all._
import io.circe.syntax._
import io.circe._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class UserRoutesMTL[F[_], R, E <: Throwable](resourceAlgebra: ResourceAlgebra[F, R])(implicit F: Sync[F],
                                                                                     E: Encoder[R],
                                                                                     D: Decoder[R],
                                                                                     H: HttpErrorHandler[F, E])
    extends Http4sDsl[F] {

  def getAll: HttpRoutes[F] =
    H.handle {
      HttpRoutes.of[F] {
        case GET -> Root =>
          Ok(resourceAlgebra.list.map(_.asJson))
      }
    }

  def getByID: HttpRoutes[F] =
    H.handle {
      HttpRoutes.of[F] {
        case GET -> Root / id =>
          resourceAlgebra.find(id) >>= (_.fold(NotFound(s"Not found: $id".asJson))(r => Ok(r.asJson)))
      }
    }

  def create: HttpRoutes[F] =
    H.handle {
      HttpRoutes.of[F] {
        case req @ POST -> Root =>
          req.as[R] >>= (r => resourceAlgebra.save(r)) >>= { case (id, _) => Created(id.asJson) }
      }
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

  val routes: HttpRoutes[F] = getAll <+> getByID <+> create

}

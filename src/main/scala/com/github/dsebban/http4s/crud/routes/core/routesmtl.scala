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

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root =>
      resourceAlgebra.list >>= (stream => Ok(stream.map(_.asJson)))

    case GET -> Root / id =>
      resourceAlgebra.find(id) >>= (_.fold(NotFound(s"Not found: $id".asJson))(r => Ok(r.asJson)))

    case req @ POST -> Root =>
      req.as[R] >>= (user => resourceAlgebra.save(user) >>= (user => Created(user.asJson)))

    // case req @ PUT -> Root / id =>
    //   req.as[UserUpdateAge] >>= (
    //       uu =>
    //         resourceAlgebra.update(id, _.age, uu.age, (u: User, newAge: Int) => u.copy(age = newAge)) *> Created(
    //           id.asJson
    //         )
    //   )
  }

  val routes: HttpRoutes[F] = H.handle(httpRoutes)

}

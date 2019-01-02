package com.github.dsebban.http4s.crud.routes.core

import algebra._
import domain._
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

// class UserRoutesMTL[F[_]: Sync](
//     users: UserAlg[F]
// )(implicit H: HttpErrorHandler[F, UserError])
//     extends Http4sDsl[F] {

//   private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

//     case GET -> Root / "users" / username =>
//       users.find(username).flatMap {
//         case Some(user) => Ok(user.asJson)
//         case None       => NotFound(username.asJson)
//       }

//     case req @ POST -> Root / "users" =>
//       req.as[User].flatMap { user =>
//         users.save(user) *> Created(user.username.asJson)
//       }

//     case req @ PUT -> Root / "users" / username =>
//       req.as[UserUpdateAge].flatMap { userUpdate =>
//         users.updateAge(username, userUpdate.age) *> Created(username.asJson)
//       }
//   }

//   val routes: HttpRoutes[F] = H.handle(httpRoutes)

// }

class UserRoutesMTL[F[_]](resourceAlgebra: ResourceAlgebra[F, User])(implicit F: Sync[F],
                                                                     H: HttpErrorHandler[F, UserError])
    extends Http4sDsl[F] {
  import UserError._

  // private def okJson[T: Encoder](t: T): F[Response[F]] =
  //   Ok(t.asJson)

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root =>
      resourceAlgebra.list >>= (stream => Ok(stream.map(_.asJson)))

    case GET -> Root / id =>
      resourceAlgebra.find(id) >>= (_.fold(F.raiseError[Response[F]](UserNotFound(id)))(r => Ok(r.asJson)))

    case req @ POST -> Root =>
      req.as[User] >>= (user => resourceAlgebra.save(user) >>= (user => Created(user.asJson)))

    case req @ PUT -> Root / id =>
      req.as[UserUpdateAge] >>= (
          uu =>
            resourceAlgebra.update(id, _.age, uu.age, (u: User, newAge: Int) => u.copy(age = newAge)) *> Created(
              id.asJson
            )
      )
  }

  val routes: HttpRoutes[F] = H.handle(httpRoutes)

}

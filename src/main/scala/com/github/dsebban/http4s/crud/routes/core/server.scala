package com.github.dsebban.http4s.crud.routes.core

import algebra._
import cats.effect._
import cats.data.EitherNel
import com.olegpy.meow.hierarchy._
import org.http4s._
import org.http4s.server._
import io.circe._

object server {

  class HttpServer[F[_]: Sync, R: Encoder: Decoder, E <: Throwable](repo: ResourceAlgebra[F, R], prefix: String)(
      implicit H: HttpErrorHandler[F, E]
  ) {

    import org.http4s.implicits._
    import org.http4s.server.Router

    val routes: HttpRoutes[F] = Router(prefix -> new UserRoutesMTL[F, R, E](repo).routes)

    val httpApp: HttpApp[F] = routes.orNotFound

  }

  class AuthedHttpServer[F[_]: Sync, R: Encoder: Decoder, E <: Throwable, ME <: Throwable, A](
      repo: AuthedResourceAlgebra[F, R, A],
      middleware: AuthMiddleware[F, A],
      validator: (R, A) => EitherNel[E, R],
      prefix: String
  )(
      implicit H: HttpErrorHandler[F, E],
      J: HttpErrorHandler[F, ME]
  ) {

    import org.http4s.implicits._
    import org.http4s.server.Router

    val routes: HttpRoutes[F] = Router(
      prefix -> J.handle(new UserAuthedRoutesMTL[F, R, E, ME, A](repo, middleware, validator).routes)
    )

    val httpApp: HttpApp[F] = routes.orNotFound

  }
}

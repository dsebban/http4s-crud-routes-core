package com.github.dsebban.http4s.crud.routes.core

import algebra._
import cats.effect._
import com.olegpy.meow.hierarchy._
import org.http4s._
import io.circe._

object server {

  class HttpServer[F[_]: Sync, R: Encoder: Decoder, E <: Throwable](repo: ResourceAlgebra[F, R], prefix: String)(
      implicit H: HttpErrorHandler[F, E]
  ) {

    import org.http4s.implicits._
    import org.http4s.server.Router

    val routes = Router(prefix -> new UserRoutesMTL[F, R, E](repo).routes)

    val httpApp: HttpApp[F] = routes.orNotFound

  }
}

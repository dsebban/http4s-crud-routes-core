package com.github.dsebban.http4s.crud.routes.core

import algebra._
import domain._
import cats.effect._
import cats.syntax.functor._
import com.olegpy.meow.hierarchy._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

object server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    interpreter.create[IO].flatMap { users =>
      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(new HttpServer[IO](users).httpApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }

}

class HttpServer[F[_]: Sync](users: ResourceAlgebra[F, User]) {
  import domain._
  import io.circe.generic.auto._
  import org.http4s.server.Router

  implicit def errorHandler: HttpErrorHandler[F, UserError] = new UserError.UserHttpErrorHandler[F]

  val routes = Router("/users" -> new UserRoutesMTL[F, User, UserError](users).routes)

  val httpApp: HttpApp[F] = routes.orNotFound

}

package com.github.dsebban.http4s.crud.routes.core

import domain._
import server._
import cats.effect._
// import cats.data._
import cats.syntax.functor._
import com.olegpy.meow.hierarchy._
import io.circe.generic.auto._
// import org.http4s.server._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import com.github.dsebban.http4s.crud.routes.mongo.interpreter
import com.github.dsebban.http4s.crud.routes.mongo.KVStore

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val imMemoryKVStore = KVStore.createInMemory[IO, User]
    // val usersRepo: IO[algebra.ResourceAlgebra[IO, User]] = interpreter
    //   .toResourceAlgebraF[IO, User](_ => java.util.UUID.randomUUID.toString)
    // // case class Metadata(user: String)
    // // val middleware: AuthMiddleware[IO, Metadata] = auth.create(_ => Right(Metadata("a"))).middleware
    // val routes: algebra.ResourceAlgebra[IO, User] => HttpRoutes[IO] = users =>
    //   new HttpServer[IO, User, UserError](users, "/users").routes

    import org.http4s.server._
    import cats.data._
    val usersRepo: IO[algebra.AuthedResourceAlgebra[IO, User, AuthInfo]] = interpreter
      .toAuthResourceAlgebraF[IO, User, AuthInfo](_ => java.util.UUID.randomUUID.toString)

    val authUser: Kleisli[OptionT[IO, ?], Request[IO], AuthInfo] =
      Kleisli(_ => OptionT.liftF(IO(AuthInfo("bigpanda", "daniel"))))
    val routes: algebra.AuthedResourceAlgebra[IO, User, AuthInfo] => HttpRoutes[IO] = users =>
      new AuthedHttpServer[IO, User, UserError, AuthInfo](users, AuthMiddleware(authUser), "/users").routes

    usersRepo.flatMap { users =>
      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(routes(users).orNotFound)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}

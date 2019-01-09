package com.github.dsebban.http4s.crud.routes.core

import domain._
import server._
import cats.effect._
import cats.syntax.functor._
import com.olegpy.meow.hierarchy._
import io.circe.generic.auto._
import org.http4s.server.blaze.BlazeServerBuilder
import com.github.dsebban.http4s.crud.routes.mongo.interpreter
import com.github.dsebban.http4s.crud.routes.mongo.KVStore

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val imMemoryKVStore = KVStore.createInMemory[IO, User]
    val usersRepo: IO[algebra.ResourceAlgebra[IO, User]] = interpreter
      .toResourceAlgebraF[IO, User](_ => java.util.UUID.randomUUID.toString)

    usersRepo.flatMap { users =>
      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(new HttpServer[IO, User, UserError](users, "/users").httpApp)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }

}

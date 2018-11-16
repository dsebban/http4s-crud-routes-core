package com.github.dsebban.http4s.crud.routes.core

import domain._
import server._
import cats.effect._
import cats.data._
import cats.syntax.functor._
import com.olegpy.meow.hierarchy._

import io.circe.generic.auto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server._
import fs2.Stream
import com.github.dsebban.http4s.crud.routes.mongo.interpreter
import com.github.dsebban.http4s.crud.routes.mongo.KVStore
import com.github.dsebban.http4s.crud.routes.mongo.MongoHelpers._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    import reactivemongo.bson._

    import com.github.dsebban.http4s.crud.routes.mongo.MongoHelpers

    implicit val userReader = Macros.reader[User]
    implicit val userWriter = Macros.writer[User]

    val dbName = "db"

    Stream
      .resource(MongoHelpers.getDB[IO]("mongodb://mongodb:27017", dbName))
      // .eval(KVStore.createInMemory[IO, User, AuthInfo])
      .flatMap {
        // store =>
        db =>
          val store = KVStore.create[IO, User, AuthInfo, MongoError](db, _.organization)
          val usersRepo: algebra.AuthedResourceAlgebra[IO, User, AuthInfo] = interpreter
            .toAuthResourceAlgebra[IO, User, AuthInfo](store, _ => BSONObjectID.generate().stringify)

          val authUser: Kleisli[OptionT[IO, ?], Request[IO], AuthInfo] =
            Kleisli { request =>
              val headers: Map[String, String] = request.headers.toList.map(h => h.name.toString -> h.value).toMap
              OptionT.fromOption(for {
                org <- headers.get("organization")
                userName <- headers.get("user_name")
                auth = AuthInfo(org, userName)
              } yield auth)
            }

          val routes: algebra.AuthedResourceAlgebra[IO, User, AuthInfo] => HttpRoutes[IO] = users =>
            new AuthedHttpServer[IO, User, UserError, MongoError, AuthInfo](
              users,
              AuthMiddleware(authUser),
              UserError.validate,
              "/users"
            ).routes

          BlazeServerBuilder[IO]
            .bindHttp(8080, "0.0.0.0")
            .withHttpApp(routes(usersRepo).orNotFound)
            .serve
      }
      .compile
      .drain
      .as(ExitCode.Success)
  }

}

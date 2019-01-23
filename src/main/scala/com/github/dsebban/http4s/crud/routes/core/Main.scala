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
    import reactivemongo.bson._
    import scala.concurrent.Future

    import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }
    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val userReader = Macros.reader[User]
    implicit val userWriter = Macros.writer[User]

    // Connect to the database: Must be done only once per application

    val db: IO[DefaultDB] = IO.fromFuture(IO {
      val mongoUri  = "mongodb://127.0.0.1:27017"
      val driver    = MongoDriver()
      val parsedUri = MongoConnection.parseURI(mongoUri)
      Future.fromTry(parsedUri.map(driver.connection(_))).flatMap(_.database("db"))
    })

    implicit val mongoKVStore = KVStore.create[IO, User, AuthInfo](db, _.organization)
    // implicit val imMemoryKVStore = KVStore.createInMemory[IO, User, AuthInfo]

    // val usersRepo: IO[algebra.ResourceAlgebra[IO, User]] = interpreter
    //   .toResourceAlgebraF[IO, User](_ => java.util.UUID.randomUUID.toString)
    // // case class Metadata(user: String)
    // // val middleware: AuthMiddleware[IO, Metadata] = auth.create(_ => Right(Metadata("a"))).middleware
    // val routes: algebra.ResourceAlgebra[IO, User] => HttpRoutes[IO] = users =>
    //   new HttpServer[IO, User, UserError](users, "/users").routes

    import org.http4s.server._
    import cats.data._
    val usersRepo: IO[algebra.AuthedResourceAlgebra[IO, User, AuthInfo]] = interpreter
      .toAuthResourceAlgebraF[IO, User, AuthInfo](_ => BSONObjectID.generate().stringify)

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
      new AuthedHttpServer[IO, User, UserError, AuthInfo](users, AuthMiddleware(authUser), UserError.validate, "/users").routes

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

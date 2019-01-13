package com.github.dsebban.http4s.crud.routes.middleware

import cats.data._
import org.http4s._
import cats.effect.IO
import org.http4s.dsl.io._
import org.http4s.server._

object auth {

  trait CustomHeaderAuth[F[_], A] {

    def auth: Kleisli[F, Request[F], Either[String, A]]
    def middleware: AuthMiddleware[F, A]

  }

  def create[A](fromHeader: List[(String, String)] => Either[String, A]) =
    new CustomHeaderAuth[IO, A] {
      def auth: Kleisli[IO, Request[IO], Either[String, A]] =
        Kleisli({ request =>
          IO.pure(fromHeader(request.headers.toList.map(h => (h.name.toString, h.value))))

        })

      val onFailure: AuthedService[String, IO] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))

      def middleware: AuthMiddleware[IO, A] = AuthMiddleware(auth, onFailure)

    }

  // val authUser: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli(_ => IO(???))

  // val onFailure: AuthedService[String, IO] = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))
  // val middleware                           = AuthMiddleware(authUser, onFailure)

  // val service: HttpRoutes[IO] = middleware(authedService)

}

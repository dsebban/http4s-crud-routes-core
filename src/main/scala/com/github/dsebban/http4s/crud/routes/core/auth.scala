package com.github.dsebban.http4s.crud.routes.core

import cats.effect.Sync
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.authentication.BasicAuth

object auth {

 // Use this header --> Authorization: Basic Z3ZvbHBlOjEyMzQ1Ng==
class BasicAuthHttpEndpoint[F[_]](implicit F: Sync[F]) extends Http4sDsl[F] {

  private val authedService: AuthedService[BasicCredentials, F] = AuthedService {
    case GET -> Root as user =>
      Ok(s"Access Granted: ${user.username}")
  }

  private val authMiddleware: AuthMiddleware[F, BasicCredentials] =
    BasicAuth[F, BasicCredentials]("Protected Realm", R.find)

  val service: HttpService[F] = authMiddleware(authedService)

}
}

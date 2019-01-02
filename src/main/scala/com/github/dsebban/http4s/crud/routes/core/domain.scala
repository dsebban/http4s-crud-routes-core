package com.github.dsebban.http4s.crud.routes.core

object domain {

  case class User(username: String, age: Int)
  case class UserUpdateAge(age: Int)

  sealed trait UserError extends Exception
  case class UserAlreadyExists(username: String) extends UserError
  case class UserNotFound(username: String) extends UserError
  case class InvalidUserAge(age: Int) extends UserError

}

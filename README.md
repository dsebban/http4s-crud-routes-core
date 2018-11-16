http4s-crud-routes-core
==========

<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

## Motivation

Generate CRUD routes only from plain ADTs in a purely functional way
Allow use to reuse existing akka-http code and port it safely

## Features

- Generate POST(Create)/GET(Read)/PUT(Update)/DELETE(Delete) routes (All done except Update)
- Handle User authentication via polymorphic type (Done)
- Provide a Mongo store with mongo error to HTTP error mapping (Done using reactive mongo , should be replaced with mongosaur)
- Provide a Kafka store with error to HTTP error mapping (Not Done)
- Provide a generic Mongo/Kafka Store composing previous 2 stores to allow to send updates after each CUD operation(Not Done)
- Sort out the types to provide stronger constraints and dependency on error handling and ease of use/reasoning(Not Done)
- Display All validation error at once (Not Done)

## Use

By default it uses kube mongo 

```shell
//CREATE a good user
http POST localhost:8080/users organization:bigpanda user_name:daniel username=asd age=68 -v

//Bad age
http POST localhost:8080/users organization:bigpanda user_name:daniel username=asd age=-68 -v

//Get all 
http localhost:8080/users organization:bigpanda user_name:daniel
//not authorized 
http localhost:8080/users organization:bigpanda
//DELETE
http DELETE localhost:8080/users/5c6c0aca99cc505ba35232 organization:bigpanda user_name:aa
```

## Types

```scala

//abstract algebras
//the abstract operation on resource `R` with authed user `A`
trait AuthedResourceAlgebra[F[_], R, A] {
  def save(r: R, user: A): F[(Id, R)]
  def find(id: Id, user: A): F[Option[R]]
  def delete(id: Id, user: A): F[Unit]
  def list(user: A): Stream[F, (Id, R)]
}

//error handling
trait HttpErrorHandler[F[_], E <: Throwable] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}  
// augment the given route with error handling translate E errors inside F into HTTP errors with error codes (400/500/...)


//Compose AuthedResourceAlgebra and HttpErrorHandler and return routes
class UserAuthedRoutesMTL[F[_], R, E <: Throwable, ME <: Throwable, A](
    resourceAlgebra: AuthedResourceAlgebra[F, R, A],
    Middleware: AuthMiddleware[F, A],
    validator: (R, A) => EitherNel[E, R]
)(implicit H: HttpErrorHandler[F, E]){
   routes: HttpRoutes[F]
}

//Compose AuthedResourceAlgebra and HttpErrorHandler and return routes
class AuthedHttpServer[F[_]: Sync, R: Encoder: Decoder, E <: Throwable, ME <: Throwable, A](
      repo: AuthedResourceAlgebra[F, R, A],
      middleware: AuthMiddleware[F, A],
      validator: (R, A) => EitherNel[E, R],
      prefix: String
  )(
      implicit H: HttpErrorHandler[F, E],
      J: HttpErrorHandler[F, ME]
  )


//Mongo related types
trait KVStore[F[_], K, V, A] {
  def get(k: K, a: A): F[Option[V]]
  def put(k: K, v: V, a: A): F[Unit]
  def delete(k: K, a: A): F[Unit]
  def scan(a: A): Stream[F, (K, V)]
}
// implemented with Reactive mongo via MongoHelpers class

// transform a KVStore to AuthedResourceAlgebra
  def toAuthResourceAlgebra[F[_], V, A](
      kvStore: KVStore[F, String, V, A],
      keyGen: V => String
  )(implicit F: Sync[F]): AuthedResourceAlgebra[F, V, A] =

```

Flow of types
```scala

KVStore => AuthedResourceAlgebra => AuthedHttpServer => UserAuthedRoutesMTL => HttpRoutes => BlazeServerBuilder
```

## Design Constraints

- Error handling as first class citizen
- Decouple Error handling and HTTP4S as much as possible
- Avoid F[_] in error handling
- User should only define ADTs and Error handling all the wiring should be taken care of

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with
the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

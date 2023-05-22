package com.sidziuk.routes.player

import cats.effect.{Async, IO, Sync}
import com.sidziuk.service.player.PlayerService
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Request, Response}
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.Concurrent
import cats.implicits._
import com.sidziuk.dto.player.in.CreatePlayerDTO
import com.sidziuk.dto.player.out.PlayerUUID
import com.sidziuk.service.player.PlayerService
import io.circe.syntax.EncoderOps
import org.http4s.CharsetRange.*
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.StatusCode
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object PlayerTapirRoutes {

  def getPlayerEndPoints[F[_]: Async](playersService: PlayerService[F]) = {

    val createPlayerEndpoint: PublicEndpoint[CreatePlayerDTO, String, PlayerUUID, Any] =
      endpoint.post
        .in("player")
        .in(jsonBody[CreatePlayerDTO])
        .out(jsonBody[PlayerUUID])
        .errorOut(statusCode(StatusCode.BadRequest).and(plainBody[String]))

    val getPlayerEndpoint: PublicEndpoint[(String, String), String, PlayerUUID, Any] =
      endpoint.get
        .in("player" / path[String]("name") / path[String]("password"))
        .out(jsonBody[PlayerUUID])
        .errorOut(statusCode(StatusCode.NotFound).and(plainBody[String]))

    val deletePlayerEndpoint: PublicEndpoint[String, String, PlayerUUID, Any] =
      endpoint.delete
        .in("player" / path[String]("playerID"))
        .out(jsonBody[PlayerUUID])
        .errorOut(statusCode(StatusCode.NotFound).and(plainBody[String]))

    val createPlayerRoute: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(createPlayerEndpoint.serverLogic(playersService.create))
    val getPlayerRoute: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(getPlayerEndpoint.serverLogic{ case (name, password) => playersService.get(name, password)})
    val deletePlayerRoute: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(deletePlayerEndpoint.serverLogic(playersService.delete))

    val endpoints = List(createPlayerEndpoint, getPlayerEndpoint, deletePlayerEndpoint)
    val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[F](endpoints, "My App", "1.0")
    val createSwaggerUIRoutes: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(swaggerEndpoints)

    createPlayerRoute <+> getPlayerRoute <+> deletePlayerRoute <+> createSwaggerUIRoutes
  }
}

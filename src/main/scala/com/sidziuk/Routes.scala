//package com.sidziuk
//
//import cats.Monad
//import cats.effect.kernel.Ref
//import cats.effect.{ExitCode, IO, IOApp}
//import com.sidziuk.dto.CreatedPlayer
//import org.http4s.HttpRoutes
//import org.http4s.dsl._
//import org.http4s.dsl.io._
//import org.http4s.ember.client._
//import org.http4s.ember.server._
//import cats.data.{EitherT, Validated}
//import cats.effect.{Clock, ExitCode, IO, IOApp}
//import cats.syntax.all._
//import cats.effect.{ExitCode, IO, IOApp}
//import com.comcast.ip4s.IpLiteralSyntax
//import com.sidziuk.dto.CreatedPlayer
//import org.http4s.HttpRoutes
//import org.http4s.UrlForm.entityEncoder
//import org.http4s.dsl.io.{->, /, GET, Ok, POST, Root}
//import org.http4s.ember.server.EmberServerBuilder
//import org.http4s.server.middleware.ErrorHandling
//import cats.effect.kernel.Ref
//import cats.effect.{ExitCode, IO, IOApp, Sync}
//import cats.implicits._
//import org.http4s.HttpRoutes
//import org.http4s.dsl.io._
//import org.http4s.implicits._
//import org.http4s.server.websocket.WebSocketBuilder
//import org.http4s.websocket.WebSocketFrame
//import org.http4s.websocket.WebSocketFrame.Text
//import org.http4s.websocket.WebSocketFrame.Close
//import org.http4s.websocket.WebSocketFrame.Binary
//import cats.effect.std.Queue
//import cats.effect.unsafe.implicits.global
//import io.circe.syntax._
//import io.circe.generic.auto._
//import org.http4s.circe.CirceEntityCodec._
//
//import scala.concurrent.duration._
//import fs2.Stream
//import org.http4s.ember.server.EmberServerBuilder
//
//import java.util.UUID
//
//object Routes {
//
//  def getUUIDWithSeed(seed: Long): UUID = {
//    val mostSignificantBits = (seed << 32) | (seed >>> 32)
//    val leastSignificantBits = (seed << 48) | ((seed & 0xffffL) << 32) | (seed >>> 16)
//    new UUID(mostSignificantBits, leastSignificantBits)
//  }
//
//  val seedForPlayer = System.currentTimeMillis()
//
//  def getRoutes(players: Ref[IO, Map[UUID, Player]]): HttpRoutes[IO] = {
//    HttpRoutes.of[IO] {
//
//      case req@POST -> Root / "player" =>
//        for {
//          createdPlayer <- req.as[CreatedPlayer]
//          existingPlayers <- players.get
//          response <- existingPlayers.values.find { player =>
//            player.name == createdPlayer.name &&
//              player.password == createdPlayer.password
//          } match {
//            case Some(_) =>
//              BadRequest(s"Player with name ${createdPlayer.name} already exists")
//            case None =>
//              val playerId = UUID.randomUUID()
//              players.update(
//                _.updated(playerId, Player(playerId, createdPlayer.name, createdPlayer.password))
//              ) >>
//                Ok(playerId.asJson)
//          }
//        } yield response
//
//      case GET -> Root / "players" / name / password =>
//        for {
//          existingPlayers <- players.get
//          response <- existingPlayers.collectFirst {
//            case (playerId, player) if player.name == name && player.password == password =>
//              playerId
//          } match {
//            case Some(playerId) => Ok(playerId.asJson)
//            case None => BadRequest(s"Player with name $name and $password does not exist")
//          }
//        } yield response
//    }
//  }
//}

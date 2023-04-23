package com.sidziuk

import cats.data.{EitherT, Validated}
import cats.effect.{Clock, ExitCode, IO, IOApp}
import cats.syntax.all._
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.IpLiteralSyntax
import com.sidziuk.dto.CreatePlayerDTO
import org.http4s.HttpRoutes
import org.http4s.UrlForm.entityEncoder
import org.http4s.dsl.io.{->, /, GET, Ok, POST, Root}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.ErrorHandling
import cats.effect.kernel.Ref
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.websocket.WebSocketFrame.Close
import org.http4s.websocket.WebSocketFrame.Binary
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import com.sidziuk.game.OHellGame
import com.sidziuk.player.{OHellPlayer, PlayerImpl}
import com.sidziuk.room.GameRoom
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._

import scala.concurrent.duration._
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import io.circe._
import org.http4s.circe.jsonOf

import java.util.UUID

object Server extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {

//    val seedForPlayer = System.currentTimeMillis()
//    val seedForRoom   = System.currentTimeMillis()
//    val gameRooms     = Ref.of[IO, Map[UUID, GameRoom[IO]]](Map.empty).unsafeRunSync()
//    val players       = Ref.of[IO, Map[UUID, Player]](Map.empty)
//    val messageQueue  = Queue.bounded[IO, String](10)

    for {
      players   <- Ref.of[IO, Map[UUID, PlayerImpl]](Map.empty)
      gameRooms <- Ref.of[IO, Map[UUID, GameRoom]](Map.empty)
      exitCode  <- {
        val routes = Routesd.getRoutes(players, gameRooms).orNotFound
        val server = EmberServerBuilder
          .default[IO]
          .withHost(ipv4"127.0.0.1")
          .withPort(port"9001")
          .withHttpApp(routes)
          .build
          .useForever

        server.as(ExitCode.Success)
      }
    } yield exitCode
  }
}

object Routesd {

//  def getUUIDWithSeed(seed: Long): UUID = {
//    val mostSignificantBits  = (seed << 32) | (seed >>> 32)
//    val leastSignificantBits = (seed << 48) | ((seed & 0xffffL) << 32) | (seed >>> 16)
//    new UUID(mostSignificantBits, leastSignificantBits)
//  }
//
//  val seedForPlayer = System.currentTimeMillis()

  def getRoutes(players: Ref[IO, Map[UUID, PlayerImpl]], gameRooms: Ref[IO, Map[UUID, GameRoom]]): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {

      case req @ POST -> Root / "player" =>
        for {
          createdPlayer   <- req.as[CreatePlayerDTO]
          existingPlayers <- players.get
          response        <- existingPlayers.values.find { player =>
                               player.name == createdPlayer.name &&
                               player.password == createdPlayer.password
                             } match {
                               case Some(_) =>
                                 BadRequest(s"Player with name ${createdPlayer.name} already exists")
                               case None    =>
                                 val playerId = UUID.randomUUID()
                                 players.update(
                                   _.updated(playerId, PlayerImpl(playerId, createdPlayer.name, createdPlayer.password))
                                 ) >>
                                   Ok(playerId.asJson)
                             }
        } yield response

      case GET -> Root / "players" / name / password =>
        for {
          existingPlayers <- players.get
          response        <- existingPlayers.collectFirst {
                               case (playerId, player) if player.name == name && player.password == password =>
                                 playerId
                             } match {
                               case Some(playerId) => Ok(playerId.asJson)
                               case None           => BadRequest(s"Player with name $name and $password does not exist")
                             }
        } yield response

      case POST -> Root / "rooms" / playerID / gameType =>
        for {
          allPlayers <- players.get
          playerUUID <- IO(UUID.fromString(playerID))
          response   <- allPlayers.get(playerUUID) match {
                          case Some(player) =>
                            val roomId = UUID.randomUUID()
                            gameType match {
                              case "OHell" =>
                                val gameRoom = GameRoom(
                                  roomId,
                                  OHellGame(players = Seq(OHellPlayer(uuid = player.uuid, name = player.name)))
                                )
                                gameRooms.update(c => c.updated(roomId, gameRoom)) >>
//                                println("--")
//                                gameRooms.get.map(c => println(c))
//                                println("+++")
                                Ok(gameRoom.asJson)
                              case _       => BadRequest(s"Game with name $gameType does not exist")
                            }
                          case None         =>
                            BadRequest(s"Player with ID $playerID does not exist")
                        }
        } yield response

      case GET -> Root / "rooms" =>
        for {
          allRooms <- gameRooms.get
          response <- Ok(allRooms.filter{case (k, v) => !v.game.isGameStarted}.asJson)
        } yield response

      case POST -> Root / "rooms" / "add" / roomID / playerID =>
        for {
          allPlayers <- players.get
          allRooms   <- gameRooms.get
          playerUUID <- IO(UUID.fromString(playerID))
          roomUUID   <- IO(UUID.fromString(roomID))
          response   <- allPlayers.get(playerUUID) match {
                          case Some(player) =>
                            allRooms.get(roomUUID) match {
                              case Some(room) =>
                                if (room.game.players.map(_.uuid).contains(player.uuid)) {
                                  BadRequest(s"Player with ID $playerID already exists in room $roomID")
                                } else if (room.game.maxPlayersNumber == room.game.players.size) {
                                  BadRequest(s"Game is full of players")
                                } else {
                                  room.game match {
                                    case oHellGAme: OHellGame =>
                                      val updatedRoom = room.copy(game =
                                        oHellGAme.copy(players =
                                          (oHellGAme.players :+ OHellPlayer(
                                            uuid = player.uuid,
                                            name = player.name
                                          )).reverse
                                        )
                                      )
                                      gameRooms.update(_.updated(roomUUID, updatedRoom))
                                      Ok(updatedRoom.asJson)
                                    case _                    => BadRequest(s"Game type is not valid")
                                  }
                                }
                              case None       => BadRequest(s"Room with ID $roomID does not exist")
                            }
                          case None         => BadRequest(s"Player with ID $playerID does not exist")
                        }
        } yield response
    }
  }
}

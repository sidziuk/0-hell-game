package com.sidziuk

import cats.syntax.either._
import cats.syntax.functor._
import cats.effect.kernel.Ref
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.IpLiteralSyntax
import com.sidziuk.deck.Card
import com.sidziuk.room.GameRoom
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import fs2.concurrent._
import fs2.{Pipe, Stream}
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import com.sidziuk.domain.game.{GameRulesAlgebra, MoveType, OHellGame, OHellMove, OHellPlayer}
import com.sidziuk.domain.player.RegisteredPlayer
import com.sidziuk.repository.player.DoobiePlayerRepositoryImp
import com.sidziuk.routes.player.PlayerRoutes
import com.sidziuk.servis.player.PlayerServiceImp
import doobie.h2.H2Transactor
import doobie.implicits.toSqlInterpolator
import io.circe
import org.http4s.server.websocket.{WebSocketBuilder, WebSocketBuilder2}
import org.http4s.websocket.WebSocketFrame
import io.circe._
import io.circe.parser._
import fs2.Stream
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import doobie.{ExecutionContexts, Transactor}
import cats.effect._
import cats.implicits.toFunctorOps
import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits.toFunctorOps
import com.sidziuk.dto.WebSocketDTO
import com.sidziuk.dto.game.ohellgame.responce.{OHellGameDTO, OHellPlayerDTO}
import com.sidziuk.dto.game.room.command.{CreateNewRoomDTO, GetRoomsDTO, JoinToRoomDTO, LeaveRoomDTO, RunGameDTO}
import com.sidziuk.dto.game.room.responce.GameRoomDTO
import com.sidziuk.dto.player.CreatePlayerDTO
import com.sidziuk.repository.CreatePlayerTable
import com.sidziuk.repository.DbTransactor.geth2Transactor
import com.sidziuk.repository.game.DoobieGameRepositoryImp
import com.sidziuk.routes.CommonRoutes
import com.sidziuk.routes.game.GameRoutes
import com.sidziuk.servis.game.GameServiceImp
import doobie._
import doobie.implicits._

import java.util.UUID
import scala.concurrent.ExecutionContext

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    geth2Transactor[IO].use { transactor =>
      for {
        logger <- Slf4jLogger.create[IO]
        _      <- CreatePlayerTable[IO](transactor)
        //      players    <- Ref.of[IO, Map[UUID, RegisteredPlayer]](Map.empty)
        gameRooms  <- Ref.of[IO, Map[UUID, GameRoom[IO]]](Map.empty)
        roomsTopic <- Topic[IO, String]
        h2PlayerRepository  = DoobiePlayerRepositoryImp[IO](transactor)
        playerService = new PlayerServiceImp[IO](h2PlayerRepository)
        playerRoutes  = new PlayerRoutes[IO](playerService).getPlayerRoutes

        h2GameRepository  = DoobieGameRepositoryImp[IO](transactor)
        gameService = new GameServiceImp[IO](h2GameRepository)



        exitCode <- {
          val server = EmberServerBuilder
            .default[IO]
            .withHost(ipv4"127.0.0.1")
            .withPort(port"9001")
          .withHttpWebSocketApp(ws =>
            CommonRoutes.getAllRoutes(
            playersService = playerService,
            gameService = gameService,
            gameRooms = gameRooms,
            roomsTopic = roomsTopic,
            logger = logger,
            ws = ws
          ))
//            .withHttpApp(playerRoutes.orNotFound)
            .build
            .useForever

          server.as(ExitCode.Success)
        }
      } yield exitCode
    }
  }
}

object Routesd {

//  def getUUIDWithSeed(seed: Long): UUID = {
//    val mostSignificantBits  = (seed << 32) | (seed >>> 32)
//    val leastSignificantBits = (seed << 48) | ((seed & 0xffffL) << 32) | (seed >>> 16)
//    new UUID(mostSignificantBits, leastSignificantBits)
//  }
//
//    val seedForPlayer = System.currentTimeMillis()
//    val seedForRoom   = System.currentTimeMillis()
//    val gameRooms     = Ref.of[IO, Map[UUID, GameRoom[IO]]](Map.empty).unsafeRunSync()
//    val players       = Ref.of[IO, Map[UUID, Player]](Map.empty)
//    val messageQueue  = Queue.bounded[IO, String](10)

  def getRoutes(
    players: Ref[IO, Map[UUID, RegisteredPlayer]],
    gameRooms: Ref[IO, Map[UUID, GameRoom[IO]]],
    roomsTopic: Topic[IO, String],
    logger: SelfAwareStructuredLogger[IO],
    ws: WebSocketBuilder2[IO]
  ): HttpRoutes[IO] = {
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
                                   _.updated(playerId, RegisteredPlayer(playerId, createdPlayer.name, createdPlayer.password))
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

      case GET -> Root / "ws" / "room" / playerUUID =>
        for {
          queue       <- Queue.unbounded[IO, String]
          allPlayers  <- players.get
          playerUUID  <- IO(UUID.fromString(playerUUID))
          mayBeSocket <- allPlayers.get(playerUUID) match {
                           case Some(player) =>
                             ws.build(
                               receive = _.evalMap { case WebSocketFrame.Text(message, _) =>
                                 println(s"$message")

                                 decode[WebSocketDTO](message) match {

                                   case Right(webSocketDTO) =>
                                     println(s"${webSocketDTO.getClass}")
                                     webSocketDTO match {
                                       case GetRoomsDTO()              => queue.offer("get_rooms")
                                       case CreateNewRoomDTO(gameType) =>
                                         for {
                                           _ <- gameType match {
                                                  case "OHell" =>
                                                    val roomUUID = UUID.randomUUID()
                                                    val gameRoom = GameRoom[IO](
                                                      roomUUID = roomUUID,
                                                      game = OHellGame(players =
                                                        Seq(OHellPlayer(uuid = player.uuid, name = player.name))
                                                      )
                                                    )
                                                    gameRooms.update(c => c.updated(roomUUID, gameRoom)) >>
                                                      roomsTopic.publish1(s"new room $roomUUID was created")
                                                  case _       =>
                                                    logger.warn(
                                                      s"Game with name $gameType does not exist"
                                                    ) >> IO.unit
                                                }

                                         } yield ()
                                       case JoinToRoomDTO(_, roomUUID) =>
                                         for {
                                           allRooms <- gameRooms.get
                                           _        <- allRooms.get(roomUUID) match {
                                                         case Some(room) =>
                                                           if (
                                                             room.game.players.map(_.uuid).contains(player.uuid) &&
                                                             !room.game.isGameStarted
                                                           ) {
                                                             logger.warn(
                                                               s"Player with ID $playerUUID already exists in room $roomUUID"
                                                             ) >> IO.unit
                                                           } else if (room.game.maxPlayersNumber == room.game.players.size) {
                                                             logger.warn(s"Game is full of players") >> IO.unit
                                                           } else {
                                                             room.game match {
                                                               case oHellGAme: OHellGame =>
                                                                 println("here")
                                                                 val updatedRoom = room.copy(game =
                                                                   oHellGAme.copy(players =
                                                                     (oHellGAme.players :+ OHellPlayer(
                                                                       uuid = player.uuid,
                                                                       name = player.name
                                                                     )).reverse
                                                                   )
                                                                 )
                                                                 gameRooms.update(_.updated(roomUUID, updatedRoom)) >>
                                                                   roomsTopic.publish1(
                                                                     s"player $playerUUID joined to romm $roomUUID"
                                                                   )
                                                               case _                    => logger.warn(s"Game type is not valid") >> IO.unit
                                                             }
                                                           }
                                                         case None       =>
                                                           logger.warn(s"Room with ID $roomUUID does not exist") >> IO.unit
                                                       }

                                         } yield ()
                                       case LeaveRoomDTO(_, roomUUID)  =>
                                         for {
                                           allRooms <- gameRooms.get
                                           _        <- allRooms.get(roomUUID) match {
                                                         case Some(room) =>
                                                           if (
                                                             !room.game.players.map(_.uuid).contains(player.uuid) &&
                                                             !room.game.isGameStarted
                                                           ) {
                                                             logger.warn(
                                                               s"Player with ID $playerUUID already not exists in room $roomUUID"
                                                             ) >> IO.unit
                                                           } else {
                                                             room.game match {
                                                               case oHellGAme: OHellGame =>
                                                                 val updatedRoom = room.copy(game =
                                                                   oHellGAme.copy(players =
                                                                     oHellGAme.players.filterNot(_.uuid == playerUUID)
                                                                   )
                                                                 )
                                                                 gameRooms.update(_.updated(roomUUID, updatedRoom)) >>
                                                                   roomsTopic.publish1(
                                                                     s"player $playerUUID leaved room $roomUUID"
                                                                   )
                                                               case _                    => logger.warn(s"Game type is not valid") >> IO.unit
                                                             }
                                                           }
                                                         case None       =>
                                                           logger.warn(s"Room with ID $roomUUID does not exist") >> IO.unit
                                                       }
                                         } yield ()
                                       case RunGameDTO(_, roomUUID)    =>
                                         for {
                                           topic    <- Topic[IO, String]
                                           allRooms <- gameRooms.get
                                           _        <- allRooms.get(roomUUID) match {
                                                         case Some(room) =>
                                                           if (
                                                             room.game.players.map(_.uuid).contains(player.uuid) &&
                                                             room.game.minPlayerNumber <= room.game.players.size
                                                           ) {
                                                             room.game match {
                                                               case oHellGAme: OHellGame =>
                                                                 val updatedRoom = room.copy(
                                                                   gameTopic = Option(topic),
                                                                   game =
                                                                     GameRulesAlgebra.gameRulesAlgebraSync.startGame(oHellGAme)
                                                                 )
                                                                 gameRooms.update(_.updated(roomUUID, updatedRoom)) >>
                                                                   roomsTopic.publish1(
                                                                     s"player $playerUUID leaved room $roomUUID"
                                                                   )
                                                               case _                    => logger.warn(s"Game type is not valid") >> IO.unit
                                                             }
                                                           } else {
                                                             logger.warn(
                                                               s"Player with ID $playerUUID already not exists in room $roomUUID"
                                                             ) >> IO.unit
                                                           }
                                                         case None       =>
                                                           logger.warn(s"Room with ID $roomUUID does not exist") >> IO.unit
                                                       }
                                         } yield ()
                                     }
                                   case Left(error)         => IO.unit
                                 }
                               },
                               send = {
                                 val topicStream = roomsTopic.subscribe(maxQueued = 10)
                                 val queueStream = Stream.repeatEval(queue.take)
                                 Stream(topicStream, queueStream).parJoinUnbounded.evalMap { _ =>
                                   for {
                                     allRooms <- gameRooms.get
                                   } yield WebSocketFrame.Text(
                                     allRooms
                                       .filter { case (_, gameRoom) =>
                                         !gameRoom.game.isGameStarted
                                       }
                                       .map { case (uuid, gameRoom) =>
                                         uuid -> GameRoomDTO(roomUUID = gameRoom.roomUUID, game = gameRoom.game)
                                       }
                                       .asJson
                                       .noSpaces
                                   )
                                 }
                               }
                             )
                           case None         => BadRequest(s"Player with ID $playerUUID does not exist")
                         }

        } yield mayBeSocket

      case GET -> Root / "ws" / "game" / playerUUID / roomUUID =>
        for {
          allPlayers  <- players.get
          playerUUID  <- IO(UUID.fromString(playerUUID))
          mayBeSocket <- allPlayers.get(playerUUID) match {
                           case Some(player) =>
                             for {
                               allRooms     <- gameRooms.get
                               roomUUID     <- IO(UUID.fromString(roomUUID))
                               mayBeSocket1 <- allRooms.get(roomUUID) match {
                                                 case Some(room) =>
                                                   val gameTopic = room.gameTopic.get
                                                   ws.build(
                                                     receive = _.evalMap { case WebSocketFrame.Text(message, _) =>
                                                       decode[WebSocketDTO](message) match {
                                                         case Right(webSocketDTO) =>
                                                           println(s"${webSocketDTO.getClass}")
                                                           for {
                                                             allRooms <- gameRooms.get
                                                             _        <- allRooms.get(roomUUID) match {
                                                                           case Some(currentRoom) =>
                                                                             currentRoom.game match {
                                                                               case oHellGame: OHellGame =>
                                                                                 webSocketDTO match {
                                                                                   case move: OHellMove =>
                                                                                     val newGame     =
                                                                                       GameRulesAlgebra.gameRulesAlgebraSync
                                                                                         .makeMove(oHellGame, move)
                                                                                     println(newGame)
                                                                                     val updatedRoom = currentRoom.copy(
                                                                                       game = newGame
                                                                                     )
                                                                                     gameRooms.update(
                                                                                       _.updated(roomUUID, updatedRoom)
                                                                                     ) >>
                                                                                       gameTopic.publish1(
                                                                                         s"player $playerUUID leaved room $roomUUID"
                                                                                       ) >> IO.unit
                                                                                   case GetRoomsDTO()   =>
                                                                                     println("stars-----" + currentRoom)
                                                                                     currentRoom.gameTopic.get.publish1(
                                                                                       s"player $playerUUID leaved room $roomUUID"
                                                                                     ) >> IO.unit
                                                                                   case _               => IO.unit
                                                                                 }
                                                                               case _                    => IO.unit
                                                                             }
                                                                           case None              => IO.unit
                                                                         }

                                                           } yield ()

                                                         case Left(error) => IO.unit
                                                       }
                                                     },
                                                     send = {

                                                       gameTopic
                                                         .subscribe(maxQueued = 10)
                                                         .evalMap { _ =>
                                                           for {
                                                             allRooms <- gameRooms.get
                                                           } yield {
                                                             val text = allRooms.get(roomUUID) match {
                                                               case Some(room) =>
                                                                 room.game match {
                                                                   case oHellGame: OHellGame =>
                                                                     OHellGameDTO(
                                                                       cardNumberInDeck = oHellGame.deck.get.cards.size,
                                                                       trump = oHellGame.deck.get.trump.get,
                                                                       player = oHellGame.players.map { player =>
                                                                         OHellPlayerDTO(
                                                                           uuid = player.uuid,
                                                                           name = player.name,
                                                                           hand =
                                                                             if (player.uuid == playerUUID)
                                                                               player.hand
                                                                             else None,
                                                                           cardNumberOnHand = player.hand match {
                                                                             case Some(value) => Option(value.size)
                                                                             case None        => None
                                                                           },
                                                                           bid = player.bid,
                                                                           possibleBids = player.possibleBids,
                                                                           tricksNumber = player.tricksNumber,
                                                                           isDealer = player.isDealer,
                                                                           isHaveMove = player.isHaveMove,
                                                                           score = player.score
                                                                         )
                                                                       },
                                                                       winner = oHellGame.winner,
                                                                       desk = oHellGame.desk,
                                                                       numberCardsOnHands =
                                                                         oHellGame.numberCardsOnHands,
                                                                       scoreHistory = oHellGame.scoreHistory,
                                                                       currentGameRound = oHellGame.currentGameRound,
                                                                       moveType = oHellGame.moveType,
                                                                       ifGameEnd = oHellGame.ifGameEnd,
                                                                       isGameStarted = oHellGame.isGameStarted,
                                                                       minPlayerNumber = oHellGame.minPlayerNumber,
                                                                       maxPlayersNumber = oHellGame.maxPlayersNumber,
                                                                       deskWinner = oHellGame.deskWinner
                                                                     ).asJson.noSpaces
                                                                   case _                    => ""
                                                                 }
                                                               case None       => ""
                                                             }
                                                             WebSocketFrame.Text(text)
                                                           }
                                                         }

                                                     }
                                                   )
                                                 case None       => BadRequest(s"GameRoom with ID $roomUUID does not exist")
                                               }
                             } yield mayBeSocket1

                           case None => BadRequest(s"Player with ID $playerUUID does not exist")
                         }

        } yield mayBeSocket
    }
  }
}
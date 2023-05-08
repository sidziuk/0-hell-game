package com.sidziuk.routes.game

import cats.effect._
import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.implicits.toFunctorOps
import cats.syntax.all._
import com.sidziuk.domain.game.{GameRulesAlgebra, OHellGame, OHellMove, OHellPlayer}
import com.sidziuk.dto.WebSocketDTO
import com.sidziuk.dto.game.ohellgame.responce.{OHellGameDTO, OHellPlayerDTO}
import com.sidziuk.dto.game.room.command._
import com.sidziuk.dto.game.room.responce.GameRoomDTO
import com.sidziuk.room.GameRoom
import com.sidziuk.servis.game.GameServiceImp
import fs2.Stream
import fs2.concurrent._
import io.circe.parser._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.typelevel.log4cats.SelfAwareStructuredLogger

import java.util.UUID

object GameRoutes {

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

  def getRoutes[F[_]: Async: Concurrent](
    gameService: GameServiceImp[F],
    gameRooms: Ref[F, Map[UUID, GameRoom[F]]],
    roomsTopic: Topic[F, String],
    logger: SelfAwareStructuredLogger[F],
    ws: WebSocketBuilder2[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "ws" / "room" / playerUUID =>
        for {
          queue       <- Queue.unbounded[F, String]
          allPlayers  <- gameService.getAllPlayers()
          playerUUID  <- Async[F].delay(UUID.fromString(playerUUID))
          _           <- Async[F].delay(println(allPlayers, playerUUID))
          mayBeSocket <- allPlayers.get(playerUUID) match {
                           case Some(player) =>
                             println("here")
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
                                                    val gameRoom = GameRoom[F](
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
                                                    ) >> Async[F].unit
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
                                                             ) >> Async[F].unit
                                                           } else if (room.game.maxPlayersNumber == room.game.players.size) {
                                                             logger.warn(s"Game is full of players") >> Async[F].unit
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
                                                               case _                    =>
                                                                 logger.warn(s"Game type is not valid") >> Async[F].unit
                                                             }
                                                           }
                                                         case None       =>
                                                           logger
                                                             .warn(s"Room with ID $roomUUID does not exist") >> Async[F].unit
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
                                                             ) >> Async[F].unit
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
                                                               case _                    =>
                                                                 logger.warn(s"Game type is not valid") >> Async[F].unit
                                                             }
                                                           }
                                                         case None       =>
                                                           logger
                                                             .warn(s"Room with ID $roomUUID does not exist") >> Async[F].unit
                                                       }
                                         } yield ()
                                       case RunGameDTO(_, roomUUID)    =>
                                         for {
                                           topic    <- Topic[F, String]
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
                                                               case _                    =>
                                                                 logger.warn(s"Game type is not valid") >> Async[F].unit
                                                             }
                                                           } else {
                                                             logger.warn(
                                                               s"Player with ID $playerUUID already not exists in room $roomUUID"
                                                             ) >> Async[F].unit
                                                           }
                                                         case None       =>
                                                           logger
                                                             .warn(s"Room with ID $roomUUID does not exist") >> Async[F].unit
                                                       }
                                         } yield ()
                                     }
                                   case Left(error)         => Async[F].unit
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
          allPlayers  <- gameService.getAllPlayers()
          playerUUID  <- Async[F].delay(UUID.fromString(playerUUID))
          mayBeSocket <- allPlayers.get(playerUUID) match {
                           case Some(player) =>
                             for {
                               allRooms     <- gameRooms.get
                               roomUUID     <- Async[F].delay(UUID.fromString(roomUUID))
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
                                                                                       ) >> Async[F].unit
                                                                                   case GetRoomsDTO()   =>
                                                                                     println("stars-----" + currentRoom)
                                                                                     currentRoom.gameTopic.get.publish1(
                                                                                       s"player $playerUUID leaved room $roomUUID"
                                                                                     ) >> Async[F].unit
                                                                                   case _               => Async[F].unit
                                                                                 }
                                                                               case _                    => Async[F].unit
                                                                             }
                                                                           case None              => Async[F].unit
                                                                         }

                                                           } yield ()

                                                         case Left(error) => Async[F].unit
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

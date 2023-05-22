package com.sidziuk.service.room

import cats.effect.kernel.Ref
import cats.effect.{Async, Concurrent}
import cats.syntax.all._
import com.sidziuk.domain.game.{GameRulesAlgebra, OHellGame, OHellPlayer}
import com.sidziuk.domain.player.RegisteredPlayer
import com.sidziuk.domain.room.GameRoom
import fs2.concurrent.Topic
import org.typelevel.log4cats.SelfAwareStructuredLogger

import java.util.UUID
class RoomServiceHelperImp[F[_]: Async: Concurrent](
  gameRooms: Ref[F, Map[UUID, GameRoom[F]]],
  roomsTopic: Topic[F, String],
) extends RoomServiceHelper[F] {
  override def createNewRoom(gameType: String, player: RegisteredPlayer): F[Unit] = for {
    _ <- gameType match {
           case "OHell" =>
             val roomUUID = UUID.randomUUID()
             val gameRoom = GameRoom[F](
               roomUUID = roomUUID,
               game = OHellGame(players = Seq(OHellPlayer(uuid = player.uuid, name = player.name)))
             )
             gameRooms.update(c => c.updated(roomUUID, gameRoom)) >>
               roomsTopic.publish1(s"new room $roomUUID was created")
           case _       =>
//             logger.warn(
//               s"Game with name $gameType does not exist"
//             ) >>
               Async[F].unit
         }

  } yield ()

  override def joinToRoom(roomUUID: UUID, player: RegisteredPlayer): F[Unit] = for {
    allRooms <- gameRooms.get
    _        <- allRooms.get(roomUUID) match {
                  case Some(room) =>
                    if (
                      room.game.players.map(_.uuid).contains(player.uuid) &&
                      !room.game.isGameStarted
                    ) {
//                      logger.warn(
//                        s"Player with ID ${player.uuid} already exists in room $roomUUID"
//                      ) >>
                        Async[F].unit
                    } else if (room.game.maxPlayersNumber == room.game.players.size) {
//                      logger.warn(s"Game is full of players") >>
                        Async[F].unit
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
                              s"player ${player.uuid} joined to romm $roomUUID"
                            )
                        case _                    =>
//                          logger.warn(s"Game type is not valid") >>
                            Async[F].unit
                      }
                    }
                  case None       =>
//                    logger
//                      .warn(s"Room with ID $roomUUID does not exist") >>
                      Async[F].unit
                }

  } yield ()

  override def leaveRoom(roomUUID: UUID, player: RegisteredPlayer): F[Unit] = for {
    allRooms <- gameRooms.get
    _        <- allRooms.get(roomUUID) match {
                  case Some(room) =>
                    if (
                      !room.game.players.map(_.uuid).contains(player.uuid) &&
                      !room.game.isGameStarted
                    ) {
//                      logger.warn(
//                        s"Player with ID ${player.uuid} already not exists in room $roomUUID"
//                      ) >>
                        Async[F].unit
                    } else {
                      room.game match {
                        case oHellGAme: OHellGame =>
                          val updatedRoom =
                            room.copy(game = oHellGAme.copy(players = oHellGAme.players.filterNot(_.uuid == player.uuid)))
                          gameRooms.update(_.updated(roomUUID, updatedRoom)) >>
                            roomsTopic.publish1(
                              s"player ${player.uuid} leaved room $roomUUID"
                            )
                        case _                    =>
//                          logger.warn(s"Game type is not valid") >>
                            Async[F].unit
                      }
                    }
                  case None       =>
//                    logger.warn(s"Room with ID $roomUUID does not exist") >>
                      Async[F].unit
                }
  } yield ()

  override def runGame(roomUUID: UUID, player: RegisteredPlayer): F[Unit] = for {
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
                            game = GameRulesAlgebra.gameRulesAlgebraSync.startGame(oHellGAme)
                          )
                          gameRooms.update(_.updated(roomUUID, updatedRoom)) >>
                            roomsTopic.publish1(
                              s"player ${player.uuid} leaved room $roomUUID"
                            )
                        case _                    =>
//                          logger.warn(s"Game type is not valid") >>
                            Async[F].unit
                      }
                    } else {
//                      logger.warn(
//                        s"Player with ID ${player.uuid} already not exists in room $roomUUID"
//                      ) >>
                        Async[F].unit
                    }
                  case None       =>
//                    logger
//                      .warn(s"Room with ID $roomUUID does not exist") >>
                      Async[F].unit
                }
  } yield ()
}

object RoomServiceHelperImp {
  def apply[F[_]: Async: Concurrent](
    gameRooms: Ref[F, Map[UUID, GameRoom[F]]],
    roomsTopic: Topic[F, String],
    logger: SelfAwareStructuredLogger[F]
  ): RoomServiceHelperImp[F] =
    new RoomServiceHelperImp[F](
      gameRooms: Ref[F, Map[UUID, GameRoom[F]]],
      roomsTopic: Topic[F, String]
    )

}
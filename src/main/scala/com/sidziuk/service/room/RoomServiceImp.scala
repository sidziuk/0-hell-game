package com.sidziuk.service.room

import cats.Applicative
import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.effect.{Async, Concurrent}
import cats.implicits.toFunctorOps
import cats.syntax.all._
import com.sidziuk.domain.room.GameRoom
import com.sidziuk.dto.WebSocketDTO
import com.sidziuk.dto.room.in.{CreateNewRoomDTO, GetRoomsDTO, RoomCommandsDTO, RoomCommandsEnum}
import com.sidziuk.dto.room.out.{GamePlayerDTO, GameRoomDTO}
import com.sidziuk.service.player.PlayerServiceImp
import fs2.Stream
import fs2.concurrent._
import io.circe.parser._
import io.circe.syntax._
import org.http4s.Response
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import java.util.UUID

class RoomServiceImp[F[_]: Async: Concurrent: Applicative](
    playerService: PlayerServiceImp[F],
    roomsTopic: Topic[F, String],
    gameRooms: Ref[F, Map[UUID, GameRoom[F]]]
) extends RoomService[F] {
  override def getWebSocket(
      playerUUID: String,
      ws: WebSocketBuilder2[F]
  ): F[Response[F]] = {

    val roomServiceHelper: RoomServiceHelper[F] =
      new RoomServiceHelperImp[F](gameRooms, roomsTopic)
    val dsl = Http4sDsl[F]
    import dsl._

    for {
      queue <- Queue.unbounded[F, String]
      allPlayers <- playerService.getAllPlayers()
      playerUUID <- Async[F].delay(UUID.fromString(playerUUID))
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
                    case GetRoomsDTO() => queue.offer("get_rooms")
                    case CreateNewRoomDTO(gameType) =>
                      roomServiceHelper.createNewRoom(gameType, player)
                    case RoomCommandsDTO(command, roomUUID) =>
                      command match {
                        case RoomCommandsEnum.JoinToRoom =>
                          roomServiceHelper.joinToRoom(roomUUID, player)
                        case RoomCommandsEnum.LeaveRoom =>
                          roomServiceHelper.leaveRoom(roomUUID, player)
                        case RoomCommandsEnum.RunGame =>
                          roomServiceHelper.runGame(roomUUID, player)
                      }
                  }
                case Left(error) => Async[F].unit
              }
            },
            send = {
              val topicStream = roomsTopic.subscribe(maxQueued = 100)
              val queueStream = Stream.repeatEval(queue.take)
              Stream(topicStream, queueStream).parJoinUnbounded.evalMap { _ =>
                for {
                  allRooms <- gameRooms.get
                } yield WebSocketFrame.Text(
                  allRooms
                    .filter { case (_, gameRoom) =>
                      !gameRoom.game.isGameStarted
                    }
                    .map { case (_, gameRoom) =>
                      GameRoomDTO(
                        roomUUID = gameRoom.roomUUID,
                        players = Option(
                          gameRoom.game.players.map(player =>
                            GamePlayerDTO(player.uuid, player.name)
                          )
                        )
                      )
                    }
                    .asJson
                    .noSpaces
                )
              }
            }
          )
        case None => BadRequest(s"Player with ID $playerUUID does not exist")
      }
    } yield mayBeSocket
  }
}

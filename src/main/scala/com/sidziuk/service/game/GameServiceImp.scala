package com.sidziuk.service.game
import cats.Applicative
import cats.effect.kernel.Ref
import cats.effect.{Async, Concurrent}
import cats.implicits.toFunctorOps
import cats.syntax.all._
import com.sidziuk.domain.game.{GameRulesAlgebra, OHellGame, OHellMove}
import com.sidziuk.domain.room.GameRoom
import com.sidziuk.dto.WebSocketDTO
import com.sidziuk.dto.game.ohellgame.out.{OHellGameDTO, OHellPlayerDTO}
import com.sidziuk.dto.room.in.GetRoomsDTO
import com.sidziuk.service.player.PlayerServiceImp
import io.circe.parser._
import io.circe.syntax._
import org.http4s.Response
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import java.util.UUID

class GameServiceImp[F[_]: Async: Concurrent: Applicative](
                                                            playerService: PlayerServiceImp[F],
                                                            gameRooms: Ref[F, Map[UUID, GameRoom[F]]]
) extends GameService[F] {
  override def getWebSocket(
      playerUUID: String,
      roomUUID: String,
      ws: WebSocketBuilder2[F]
  ): F[Response[F]] = {

    val dsl = Http4sDsl[F]
    import dsl._

    for {
      allPlayers <- playerService.getAllPlayers()
      playerUUID <- Async[F].delay(UUID.fromString(playerUUID))
      mayBeSocket <- allPlayers.get(playerUUID) match {
        case Some(player) =>
          for {
            allRooms <- gameRooms.get
            roomUUID <- Async[F].delay(UUID.fromString(roomUUID))
            mayBeSocket1 <- allRooms.get(roomUUID) match {
              case Some(room) =>
                val gameTopic = room.gameTopic.get
                ws.build(
                  receive = _.evalMap { case WebSocketFrame.Text(message, _) =>
                    decode[WebSocketDTO](message) match {
                      case Right(webSocketDTO) =>
                        for {
                          allRooms <- gameRooms.get
                          _ <- allRooms.get(roomUUID) match {
                            case Some(currentRoom) =>
                              currentRoom.game match {
                                case oHellGame: OHellGame =>
                                  webSocketDTO match {
                                    case move: OHellMove =>
                                      val newGame =
                                        GameRulesAlgebra.gameRulesAlgebraSync
                                          .makeMove(
                                            oHellGame,
                                            OHellMove(
                                              playerId = player.uuid,
                                              moveType = move.moveType,
                                              card = move.card,
                                              bid = move.bid
                                            )
                                          )
                                      val updatedRoom = currentRoom.copy(
                                        game = newGame
                                      )
                                      gameRooms.update(
                                        _.updated(roomUUID, updatedRoom)
                                      ) >>
                                        gameTopic.publish1(
                                          s"player $playerUUID leaved room $roomUUID"
                                        ) >> Async[F].unit
                                    case GetRoomsDTO() =>
                                      currentRoom.gameTopic.get.publish1(
                                        s"player $playerUUID leaved room $roomUUID"
                                      ) >> Async[F].unit
                                    case _ => Async[F].unit
                                  }
                                case _ => Async[F].unit
                              }
                            case None => Async[F].unit
                          }
                        } yield ()
                      case Left(_) => Async[F].unit
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
                                    cardNumberInDeck =
                                      oHellGame.deck.get.cards.size,
                                    trump = oHellGame.deck.get.trump.get,
                                    players = oHellGame.players.map { player =>
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
                                    currentGameRound =
                                      oHellGame.currentGameRound,
                                    moveType = oHellGame.moveType,
                                    ifGameEnd = oHellGame.ifGameEnd,
                                    isGameStarted = oHellGame.isGameStarted,
                                    minPlayerNumber = oHellGame.minPlayerNumber,
                                    maxPlayersNumber =
                                      oHellGame.maxPlayersNumber,
                                    deskWinner = oHellGame.deskWinner
                                  ).asJson.noSpaces
                                case _ => ""
                              }
                            case None => ""
                          }
                          WebSocketFrame.Text(text)
                        }
                      }
                  }
                )
              case None =>
                BadRequest(s"GameRoom with ID $roomUUID does not exist")
            }
          } yield mayBeSocket1
        case None => BadRequest(s"Player with ID $playerUUID does not exist")
      }
    } yield mayBeSocket
  }
}

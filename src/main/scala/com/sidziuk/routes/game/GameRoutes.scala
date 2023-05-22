package com.sidziuk.routes.game

import cats.effect._
import cats.effect.kernel.Ref
import cats.implicits.toFunctorOps
import cats.syntax.all._
import com.sidziuk.domain.game.{
  GameRulesAlgebra,
  OHellGame,
  OHellMove,
  OHellPlayer
}
import com.sidziuk.domain.room.GameRoom
import com.sidziuk.dto.WebSocketDTO
import com.sidziuk.dto.game.ohellgame.out.{OHellGameDTO, OHellPlayerDTO}
import com.sidziuk.dto.room.in.GetRoomsDTO
import com.sidziuk.service.game.GameServiceImp
import com.sidziuk.service.player.PlayerService
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
  def getRoutes[F[_]: Async: Concurrent](
      gameService: GameServiceImp[F],
      ws: WebSocketBuilder2[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "ws" / "game" / playerUUID / roomUUID =>
        gameService.getWebSocket(playerUUID, roomUUID, ws)
    }
  }
}

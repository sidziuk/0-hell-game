package com.sidziuk.service.game

import cats.Applicative
import cats.data.OptionT
import cats.effect.{Async, Concurrent}
import com.sidziuk.domain.game.{OHellGame, OHellMove}
import org.http4s.Response
import org.http4s.websocket.WebSocketFrame
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
import io.circe.parser._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.typelevel.log4cats.SelfAwareStructuredLogger

import java.util.UUID
import java.util.UUID

trait GameService[F[_]] {
  def getWebSocket(playerUUID: String, roomUUID: String, ws: WebSocketBuilder2[F]): F[Response[F]]
}

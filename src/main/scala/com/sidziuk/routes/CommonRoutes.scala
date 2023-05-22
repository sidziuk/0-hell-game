package com.sidziuk.routes

import cats.effect.kernel.Ref
import cats.effect.{Async, Concurrent}
import cats.syntax.all._
import com.sidziuk.domain.room.GameRoom
import com.sidziuk.routes.game.GameRoutes
import com.sidziuk.routes.player.PlayerTapirRoutes
import com.sidziuk.routes.room.RoomRoutes
import com.sidziuk.service.game.GameServiceImp
import com.sidziuk.service.player.PlayerService
import com.sidziuk.service.room.{RoomServiceHelper, RoomServiceImp}
import fs2.concurrent.Topic
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder2
import org.typelevel.log4cats.SelfAwareStructuredLogger

import java.util.UUID

object CommonRoutes {

  def getAllRoutes[F[_]: Concurrent: Async](
                                             playersService: PlayerService[F],
                                             gameService: GameServiceImp[F],
                                             roomService: RoomServiceImp[F],
                                             ws: WebSocketBuilder2[F]
  ): HttpApp[F] = {
//    PlayerRoutes.getRoutes(playersService) <+>
    PlayerTapirRoutes.getPlayerEndPoints[F](playersService) <+>
      RoomRoutes.getRoutes[F](
        roomService = roomService,
        ws = ws
      ) <+>
      GameRoutes.getRoutes[F](
        gameService = gameService,
        ws = ws
      )
  }.orNotFound

}

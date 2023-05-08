package com.sidziuk.routes

import cats.effect.kernel.Ref
import cats.effect.{Async, Concurrent}
import cats.syntax.all._
import com.sidziuk.room.GameRoom
import com.sidziuk.routes.game.GameRoutes
import com.sidziuk.routes.player.PlayerRoutes
import com.sidziuk.servis.game.GameServiceImp
import com.sidziuk.servis.player.PlayerServiceImp
import fs2.concurrent.Topic
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder2
import org.typelevel.log4cats.SelfAwareStructuredLogger

import java.util.UUID

object CommonRoutes {

  def getAllRoutes[F[_] : Concurrent : Async](
                                  playersService: PlayerServiceImp[F],
                                  gameService: GameServiceImp[F],
                                  gameRooms: Ref[F, Map[UUID, GameRoom[F]]],
                                  roomsTopic: Topic[F, String],
                                  logger: SelfAwareStructuredLogger[F],
                                  ws: WebSocketBuilder2[F]
                                ): HttpApp[F] = {
    new PlayerRoutes[F](playersService).getPlayerRoutes <+>
      GameRoutes.getRoutes(gameService = gameService,
      gameRooms = gameRooms, roomsTopic = roomsTopic, logger = logger, ws = ws)
  }.orNotFound

}

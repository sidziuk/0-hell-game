package com.sidziuk.routes.game

import cats.effect._
import com.sidziuk.service.game.GameServiceImp
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2

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

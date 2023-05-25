package com.sidziuk.service.game

import org.http4s.Response
import org.http4s.server.websocket.WebSocketBuilder2

trait GameService[F[_]] {
  def getWebSocket(playerUUID: String, roomUUID: String, ws: WebSocketBuilder2[F]): F[Response[F]]
}

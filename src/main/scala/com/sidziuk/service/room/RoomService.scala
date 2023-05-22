package com.sidziuk.service.room

import org.http4s.Response
import org.http4s.server.websocket.WebSocketBuilder2

trait RoomService[F[_]] {
  def getWebSocket(playerUUID: String, ws: WebSocketBuilder2[F]): F[Response[F]]

}
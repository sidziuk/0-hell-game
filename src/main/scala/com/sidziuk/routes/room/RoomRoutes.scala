package com.sidziuk.routes.room

import cats.effect._
import com.sidziuk.service.room.RoomServiceImp
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2

object RoomRoutes {
  def getRoutes[F[_]: Async: Concurrent](
      roomService: RoomServiceImp[F],
      ws: WebSocketBuilder2[F]
  ): HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] { case GET -> Root / "ws" / "room" / playerUUID =>
      roomService.getWebSocket(playerUUID, ws)
    }
  }
}

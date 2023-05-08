package com.sidziuk.dto

import cats.syntax.functor._
import com.sidziuk.domain.game.OHellMove
import com.sidziuk.dto.game.room.command._
import io.circe.Decoder


trait WebSocketDTO{}

object WebSocketDTO {
  implicit val webSocketDTODecoder: Decoder[WebSocketDTO] =
    CreateNewRoomDTO.Decoder.widen[WebSocketDTO] or
      JoinToRoomDTO.runGameDTODecoder.widen[WebSocketDTO] or
      LeaveRoomDTO.runGameDTODecoder.widen[WebSocketDTO] or
      RunGameDTO.runGameDTODecoder.widen[WebSocketDTO] or
      OHellMove.moveDecoder.widen[WebSocketDTO] or
      GetRoomsDTO.getRoomsDTODecoder.widen[WebSocketDTO]
}

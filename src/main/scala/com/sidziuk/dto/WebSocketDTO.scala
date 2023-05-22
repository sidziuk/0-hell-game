package com.sidziuk.dto

import cats.syntax.functor._
import com.sidziuk.dto.game.ohellgame.in.OHellMoveDTO
import com.sidziuk.dto.room.in.{CreateNewRoomDTO, GetRoomsDTO, RoomCommandsDTO}
import io.circe.Decoder

trait WebSocketDTO {}

object WebSocketDTO {
  implicit val webSocketDTODecoder: Decoder[WebSocketDTO] =
    CreateNewRoomDTO.Decoder.widen[WebSocketDTO] or
      RoomCommandsDTO.decoder.widen[WebSocketDTO] or
      OHellMoveDTO.Decoder.widen[WebSocketDTO] or
      GetRoomsDTO.getRoomsDTODecoder.widen[WebSocketDTO]
}

package com.sidziuk.dto.game.room.command

import com.sidziuk.dto.WebSocketDTO
import io.circe.Decoder
case class GetRoomsDTO() extends  WebSocketDTO

object GetRoomsDTO {
  implicit val getRoomsDTODecoder: Decoder[GetRoomsDTO] = Decoder.const(GetRoomsDTO())
}

package com.sidziuk.dto.room.in

import com.sidziuk.dto.WebSocketDTO
import io.circe.Decoder
case class GetRoomsDTO() extends  WebSocketDTO

object GetRoomsDTO {
  implicit val getRoomsDTODecoder: Decoder[GetRoomsDTO] = Decoder.const(GetRoomsDTO())
}

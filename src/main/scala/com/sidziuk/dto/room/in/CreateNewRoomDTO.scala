package com.sidziuk.dto.room.in

import com.sidziuk.dto.WebSocketDTO
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class CreateNewRoomDTO (gameType: String) extends WebSocketDTO

object CreateNewRoomDTO {
  implicit val Encoder: Encoder[CreateNewRoomDTO] = deriveEncoder[CreateNewRoomDTO]
  implicit val Decoder: Decoder[CreateNewRoomDTO] = deriveDecoder[CreateNewRoomDTO]
}
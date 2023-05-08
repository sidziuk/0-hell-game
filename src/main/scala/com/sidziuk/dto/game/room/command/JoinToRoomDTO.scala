package com.sidziuk.dto.game.room.command

import com.sidziuk.dto.WebSocketDTO
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class JoinToRoomDTO (joinRoom: String, roomUUID: UUID) extends WebSocketDTO

object JoinToRoomDTO {
  implicit val runGameDTOEncoder: Encoder[JoinToRoomDTO] = deriveEncoder[JoinToRoomDTO]
  implicit val runGameDTODecoder: Decoder[JoinToRoomDTO] = deriveDecoder[JoinToRoomDTO]
}
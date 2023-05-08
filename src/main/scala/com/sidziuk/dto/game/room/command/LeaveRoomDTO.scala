package com.sidziuk.dto.game.room.command

import com.sidziuk.dto.WebSocketDTO
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class LeaveRoomDTO (leaveRoom: String, roomUUID: UUID) extends WebSocketDTO

object LeaveRoomDTO {
  implicit val runGameDTOEncoder: Encoder[LeaveRoomDTO] = deriveEncoder[LeaveRoomDTO]
  implicit val runGameDTODecoder: Decoder[LeaveRoomDTO] = deriveDecoder[LeaveRoomDTO]
}
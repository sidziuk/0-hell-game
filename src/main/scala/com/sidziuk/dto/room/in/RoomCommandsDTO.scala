package com.sidziuk.dto.room.in

import com.sidziuk.dto.WebSocketDTO
import com.sidziuk.dto.room.in.RoomCommandsEnum.RoomCommands
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import java.util.UUID

case class RoomCommandsDTO (command: RoomCommands, roomUUID: UUID) extends WebSocketDTO

object RoomCommandsDTO {
  implicit val decoder: Decoder[RoomCommandsDTO] = deriveDecoder[RoomCommandsDTO]
}
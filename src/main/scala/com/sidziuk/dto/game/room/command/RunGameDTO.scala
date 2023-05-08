package com.sidziuk.dto.game.room.command

import com.sidziuk.dto.WebSocketDTO
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class RunGameDTO(runGame: String, roomUUID: UUID) extends WebSocketDTO

object RunGameDTO {
  implicit val runGameDTOEncoder: Encoder[RunGameDTO] = deriveEncoder[RunGameDTO]
  implicit val runGameDTODecoder: Decoder[RunGameDTO] = deriveDecoder[RunGameDTO]
}

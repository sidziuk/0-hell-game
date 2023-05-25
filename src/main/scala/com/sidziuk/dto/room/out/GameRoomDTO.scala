package com.sidziuk.dto.room.out

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID
case class GameRoomDTO(
    roomUUID: UUID,
    players: Option[Seq[GamePlayerDTO]] = None,
    minPlayerNumber: Int,
    maxPlayersNumber: Int
)

object GameRoomDTO {
  implicit val decoder: Decoder[GameRoomDTO] = deriveDecoder[GameRoomDTO]
  implicit val encoder: Encoder[GameRoomDTO] = deriveEncoder[GameRoomDTO]
}

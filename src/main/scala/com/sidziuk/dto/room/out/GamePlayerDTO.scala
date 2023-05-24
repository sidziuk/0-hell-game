package com.sidziuk.dto.room.out

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class GamePlayerDTO (uuid: UUID, name: String)

object GamePlayerDTO {
  implicit val decoder: Decoder[GamePlayerDTO] = deriveDecoder[GamePlayerDTO]
  implicit val encoder: Encoder[GamePlayerDTO] = deriveEncoder[GamePlayerDTO]
}

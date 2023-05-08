package com.sidziuk.dto.player

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class CreatePlayerDTO(name: String, password: String)

object CreatePlayerDTO {
  implicit val Encoder: Encoder[CreatePlayerDTO] = deriveEncoder[CreatePlayerDTO]
  implicit val Decoder: Decoder[CreatePlayerDTO] = deriveDecoder[CreatePlayerDTO]
}
package com.sidziuk.dto.player.out

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class PlayerUUID(playerUUID: String)

object PlayerUUID {
  implicit val Encoder: Encoder[PlayerUUID] = deriveEncoder[PlayerUUID]
  implicit val Decoder: Decoder[PlayerUUID] = deriveDecoder[PlayerUUID]
}
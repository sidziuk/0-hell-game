package com.sidziuk.dto.player.out

import com.sidziuk.dto.player.in.CreatePlayerDTO
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

final case class PlayerUUID(playerUUID: String)

object PlayerUUID {
  implicit val Encoder: Encoder[PlayerUUID] = deriveEncoder[PlayerUUID]
  implicit val Decoder: Decoder[PlayerUUID] = deriveDecoder[PlayerUUID]
}
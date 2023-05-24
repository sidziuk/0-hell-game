package com.sidziuk.dto.room.in

import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveEnumerationCodec

object RoomCommandsEnum {
  sealed trait RoomCommands
  case object JoinToRoom extends RoomCommands

  case object LeaveRoom extends RoomCommands

  case object RunGame extends RoomCommands

  object RoomCommands {

    implicit val roomCommandsDecoder: Decoder[RoomCommands] = deriveEnumerationCodec[RoomCommands]

  }
}

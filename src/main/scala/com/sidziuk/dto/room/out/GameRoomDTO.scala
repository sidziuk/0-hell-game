package com.sidziuk.dto.room.out

import com.sidziuk.domain.game.{Game, OHellGame}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID
case class GameRoomDTO(roomUUID: UUID, game: Game)

object GameRoomDTO {

  implicit val gameDecoder: Decoder[Game] = {
    Decoder[OHellGame].map[Game](identity)
  }

  implicit val gameEncoder: Encoder[Game] = {

    Encoder[OHellGame].asInstanceOf[Encoder[Game]]
  }
  implicit val decoder: Decoder[GameRoomDTO] = deriveDecoder[GameRoomDTO]
  implicit val encoder: Encoder[GameRoomDTO] = deriveEncoder[GameRoomDTO]
}
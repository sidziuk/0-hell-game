package com.sidziuk.dto.game.ohellgame.command

import com.sidziuk.deck.Card
import com.sidziuk.domain.game.MoveType
import com.sidziuk.dto.WebSocketDTO
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class OHellMoveDTO (moveType: MoveType, card: Option[Card], bid: Option[Int]) extends WebSocketDTO

object OHellMoveDTO {
  implicit val Encoder: Encoder[OHellMoveDTO] = deriveEncoder[OHellMoveDTO]
  implicit val Decoder: Decoder[OHellMoveDTO] = deriveDecoder[OHellMoveDTO]
}
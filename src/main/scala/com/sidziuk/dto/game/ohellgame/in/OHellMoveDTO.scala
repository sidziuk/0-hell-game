package com.sidziuk.dto.game.ohellgame.in

import com.sidziuk.domain.game.deck.Card
import com.sidziuk.domain.game.ohellgame.MoveType
import com.sidziuk.dto.WebSocketDTO
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class OHellMoveDTO (moveType: MoveType, card: Option[Card], bid: Option[Int]) extends WebSocketDTO

object OHellMoveDTO {
  implicit val Encoder: Encoder[OHellMoveDTO] = deriveEncoder[OHellMoveDTO]
  implicit val Decoder: Decoder[OHellMoveDTO] = deriveDecoder[OHellMoveDTO]
}
package com.sidziuk.dto.game.ohellgame.responce

import com.sidziuk.deck.Card
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID

case class OHellPlayerDTO(
  uuid: UUID,
  name: String,
  hand: Option[Set[Card]],
  cardNumberOnHand: Option[Int],
  bid: Option[Int],
  possibleBids: Option[Seq[Int]],
  tricksNumber: Option[Int],
  isDealer: Boolean,
  isHaveMove: Boolean,
  score: Int
)

object OHellPlayerDTO {
  implicit val Encoder: Encoder[OHellPlayerDTO] = deriveEncoder[OHellPlayerDTO]
  implicit val Decoder: Decoder[OHellPlayerDTO] = deriveDecoder[OHellPlayerDTO]
}

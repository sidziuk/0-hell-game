package com.sidziuk.dto.game.ohellgame.out

import com.sidziuk.domain.game.deck.Card
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

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

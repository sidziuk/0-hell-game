package com.sidziuk.domain.game

import com.sidziuk.domain.deck.{Card, Suit}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class Desk(cards: Option[Map[UUID, Card]] = None, firstPlayer: Option[(UUID, Suit)] = None)

object Desk {
  implicit val encoder: Encoder[Desk] = deriveEncoder[Desk]
  implicit val decoder: Decoder[Desk] = deriveDecoder[Desk]
}
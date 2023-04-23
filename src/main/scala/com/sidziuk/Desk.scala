package com.sidziuk

import com.sidziuk.Rank.{Nine, Ten}
import com.sidziuk.Suit.Heart
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class Desk(cards: Option[Map[UUID, Card]] = None, firstPlayer: Option[(UUID, Suit)] = None)

object Desk {
  implicit val encoder: Encoder[Desk] = deriveEncoder[Desk]
  implicit val decoder: Decoder[Desk] = deriveDecoder[Desk]
}

object app2 extends App {
  val f = Desk(Option(Map(UUID.randomUUID() -> Card(Heart, Nine), UUID.randomUUID() -> Card(Heart, Ten))), Option(UUID.randomUUID() -> Heart))
  println(f.asJson)
}
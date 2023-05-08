package com.sidziuk.deck

import enumeratum._
import io.circe.generic.extras.semiauto.{deriveEnumerationDecoder, deriveEnumerationEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

case class Card(suit: Suit, rank: Rank) extends Ordered[Card] {
  override def compare(that: Card): Int = {
    val suitCompare = suit.compare(that.suit)
    if (suitCompare != 0) suitCompare else rank.compare(that.rank)
  }
}

object Card {
  implicit val encoder: Encoder[Card] = (card: Card) => Json.obj(
    ("suit", card.suit.asJson),
    ("rank", card.rank.asJson)
  )

  implicit val decoder: Decoder[Card] = (c: HCursor) =>
    for {
      suit <- c.downField("suit").as[Suit]
      rank <- c.downField("rank").as[Rank]
    } yield Card(suit, rank)
}

sealed abstract class Rank(val order: Int) extends EnumEntry with Ordered[Rank] {
  def compare(that: Rank) = this.order - that.order
}
object Rank extends Enum[Rank] {

  val values = findValues

    case object Two extends Rank(0)
  case object Three extends Rank(1)
  case object Four extends Rank(2)
  case object Five extends Rank(3)
  case object Six extends Rank(4)
  case object Seven extends Rank(5)
  case object Eight extends Rank(6)
  case object Nine extends Rank(7)
  case object Ten extends Rank(8)
  case object Jack extends Rank(9)
  case object Queen extends Rank(10)
  case object King extends Rank(11)
  case object Ace extends Rank(12)

  implicit val suitEncoder: Encoder[Rank] = deriveEnumerationEncoder[Rank]
  implicit val suitDecoder: Decoder[Rank] = deriveEnumerationDecoder[Rank]


}

sealed abstract class Suit(val order: Int) extends EnumEntry with Ordered[Suit] {
  def compare(that: Suit) = this.order - that.order
}
object Suit extends Enum[Suit] {


  val values = findValues

  case object Heart extends Suit(1)
  case object Spade extends Suit(2)
  case object Diamond extends Suit(3)
  case object Club extends Suit(4)

  implicit val suitEncoder: Encoder[Suit] = deriveEnumerationEncoder[Suit]
  implicit val suitDecoder: Decoder[Suit] = deriveEnumerationDecoder[Suit]


}
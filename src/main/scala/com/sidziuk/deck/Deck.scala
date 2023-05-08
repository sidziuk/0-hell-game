package com.sidziuk.deck

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

case class Deck(cards: Set[Card], trump: Option[Card])

object Deck {

  implicit val cardEncoder: Encoder[Card] = deriveEncoder[Card]
  implicit val cardDecoder: Decoder[Card] = deriveDecoder[Card]

  implicit val deckEncoder: Encoder[Deck] = Encoder.instance { deck =>
    Json.obj(
      "cards" -> deck.cards.asJson,
      "trump" -> deck.trump.asJson
    )
  }
  implicit val deckDecoder: Decoder[Deck] = Decoder.instance { c =>
    for {
      cards <- c.downField("cards").as[Set[Card]]
      trump <- c.downField("trump").as[Option[Card]]
    } yield Deck(cards, trump)
  }
}

trait DeckAlgebra {
  def createShuffledDesk: Deck

  def deal(desk: Deck): (Card, Deck)

  def size(desk: Deck): Int

  def getTrump(desk: Deck): Deck
}

object DeckAlgebra {
  //  def apply[F[_]](implicit F: DeckAlgebra[F]): DeckAlgebra[F] = F

  implicit val deckAlgebraSync: DeckAlgebra = new DeckAlgebra {

    override def createShuffledDesk: Deck = {
      val ranks = Rank.values.toSet
      val suits = Suit.values.toSet
      val cards = for {
        rank <- ranks
        suit <- suits
      } yield Card(suit, rank)

      Deck(scala.util.Random.shuffle(cards), None)
    }

    override def deal(deck: Deck): (Card, Deck) = (deck.cards.head, Deck(deck.cards.tail, deck.trump))

    override def size(deck: Deck): Int = deck.cards.size

    override def getTrump(deck: Deck): Deck = {
      val (cards, trump) = deck.trump match {
        case Some(t) => (deck.cards, t) // There is already a trump card
        case None => (deck.cards.tail, deck.cards.head)
      }
      Deck(cards, Option(trump))
    }
  }
}


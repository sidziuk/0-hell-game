package com.sidziuk

case class Deck(cards: Set[Card], trump: Option[Card])

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

//object app extends App {
//
//
//  val d: IO[Deck] = DeckAlgebra[IO].createShuffledDesk
//  //  val f = DeckAlgebra[IO].getTrump
//
//  println(d)
//
//}


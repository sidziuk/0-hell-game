package com.sidziuk.domain.player

import cats.effect.Sync
import com.sidziuk.deck.Card

import scala.collection.immutable.SortedSet

trait Player2[F[_]] {

  def id: String

  def name: String

  def hand: Set[Card]

  def addCardToPlayerHand(card: Card): F[Player2[F]]

  def playCardFromPlayerHand(card: Card): F[(Player2[F], Option[Card])]
}

class Player2Impl[F[_]: Sync](
                               val id: String,
                               val name: String,
                               val hand: Set[Card] = SortedSet()
                             ) extends Player2[F] {

  override def addCardToPlayerHand(card: Card): F[Player2[F]] =
    Sync[F].pure(new Player2Impl(id, name, hand + card))

  override def playCardFromPlayerHand(card: Card): F[(Player2[F], Option[Card])] =
    Sync[F].pure {
      if (hand.contains(card)) (new Player2Impl(id, name, hand - card), Some(card))
      else (this, None)
    }
}

object Player2 {

  def apply[F[_]: Sync](id: String, name: String): F[Player2[F]] =
    Sync[F].pure(new Player2Impl(id, name))
}

//for{
//  pl <- Player2[IO]("sdfsdf", "Vasa" )
//  plp <- pl.addCardToPlayerHand(Card(Heart, Five))
//  g <- plp.playCardFromPlayerHand(Card(Heart, Five))
//} yield g._2



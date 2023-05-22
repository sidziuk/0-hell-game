package com.sidziuk.domain.game

import com.sidziuk.domain.Player
import com.sidziuk.domain.deck.Card
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

import java.util.UUID
import scala.collection.immutable.SortedSet


case class OHellPlayer(uuid: UUID,
                       name: String,
                       hand: Option[Set[Card]] = None,
                       bid: Option[Int] = None,
                       possibleBids: Option[Seq[Int]] = None,
                       tricksNumber: Option[Int] = None,
                       isDealer: Boolean = false,
                       isHaveMove: Boolean = false,
                       score: Int = 0) extends Player

object OHellPlayer {

  implicit val decoder: Decoder[OHellPlayer] = (c: HCursor) => {
    for {
      uuid <- c.downField("uuid").as[UUID]
      name <- c.downField("name").as[String]
      hand <- c.downField("hand").as[Option[Set[Card]]]
      bid <- c.downField("bid").as[Option[Int]]
      possibleBids <- c.downField("possibleBids").as[Option[Seq[Int]]]
      tricksNumber <- c.downField("tricksNumber").as[Option[Int]]
      isDealer <- c.downField("isDealer").as[Boolean]
      isHaveMove <- c.downField("isHaveMove").as[Boolean]
      score <- c.downField("score").as[Int]
    } yield OHellPlayer(uuid, name, hand, bid, possibleBids, tricksNumber, isDealer, isHaveMove, score)
  }

  implicit val encoder: Encoder[OHellPlayer] = (player: OHellPlayer) => Json.obj(
    ("uuid", player.uuid.asJson),
    ("name", player.name.asJson),
    ("hand", player.hand.asJson),
    ("bid", player.bid.asJson),
    ("possibleBids", player.possibleBids.asJson),
    ("tricksNumber", player.tricksNumber.asJson),
    ("isDealer", player.isDealer.asJson),
    ("isHaveMove", player.isHaveMove.asJson),
    ("score", player.score.asJson)
  )
}

trait PlayerAlgebra {
  def addCardToPlayerHand(player: OHellPlayer, card: Card): OHellPlayer

  def playCardFromPlayerHand(player: OHellPlayer, card: Card): (OHellPlayer, Option[Card])
  }

object PlayerAlgebra {
  //  def apply[F[_]](implicit F: PlayerAlgebra[F]): PlayerAlgebra[F] = F

  implicit val playerAlgebraSync: PlayerAlgebra = new PlayerAlgebra {


    override def addCardToPlayerHand(player: OHellPlayer, card: Card): OHellPlayer = player.copy(hand =
      player.hand match {
        case Some(hand) => Option(hand + card)
        case None => Option(SortedSet(card))
      }
    )

    override def playCardFromPlayerHand(player: OHellPlayer, card: Card): (OHellPlayer, Option[Card]) = player.hand match {
      case Some(hand) if hand.contains(card) => (player.copy(hand = {
        player.hand.map(_ - card) match {
          case Some(value) if value.isEmpty => None
          case hand@Some(_) => hand
        }
      }),
        Option(card))
      case None => (player, None)
    }
  }
}

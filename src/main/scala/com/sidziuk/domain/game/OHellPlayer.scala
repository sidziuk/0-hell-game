package com.sidziuk.domain.game

import com.sidziuk.deck.Card
import com.sidziuk.domain.Player
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

  implicit val decoder: Decoder[OHellPlayer] = new Decoder[OHellPlayer] {
    final def apply(c: HCursor): Decoder.Result[OHellPlayer] = {
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
  }

  implicit val encoder: Encoder[OHellPlayer] = new Encoder[OHellPlayer] {
    final def apply(player: OHellPlayer): Json = Json.obj(
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
}

trait PlayerAlgebra {
  def createPlayer(uuid: UUID, name: String): OHellPlayer

  def addCardToPlayerHand(player: OHellPlayer, card: Card): OHellPlayer

  def playCardFromPlayerHand(player: OHellPlayer, card: Card): (OHellPlayer, Option[Card])

  def setBid(player: OHellPlayer, bid: Option[Int]): OHellPlayer

  def getBid(player: OHellPlayer): Option[Int]

  def setPossibleBids(player: OHellPlayer, possibleBids: Option[Seq[Int]]): OHellPlayer

  def getPossibleBids(player: OHellPlayer): Option[Seq[Int]]

  def setTrick(player: OHellPlayer, trick: Option[Int]): OHellPlayer

  def getTrick(player: OHellPlayer): Option[Int]

  def setIsDealer(player: OHellPlayer, isDealer: Boolean): OHellPlayer

  def isDealer(player: OHellPlayer): Boolean

  def setIsHaveMove(player: OHellPlayer, isHaveMove: Boolean): OHellPlayer

  def isHaveMove(player: OHellPlayer): Boolean

  def addScore(player: OHellPlayer, score: Int): OHellPlayer

  def getScore(player: OHellPlayer): Int

}

object PlayerAlgebra {
  //  def apply[F[_]](implicit F: PlayerAlgebra[F]): PlayerAlgebra[F] = F

  implicit val playerAlgebraSync: PlayerAlgebra = new PlayerAlgebra {

    override def createPlayer(uuid: UUID, name: String): OHellPlayer = OHellPlayer(uuid, name)

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

    override def setBid(player: OHellPlayer, bid: Option[Int]): OHellPlayer = player.copy(bid = bid)

    override def getBid(player: OHellPlayer): Option[Int] = player.bid

    override def setPossibleBids(player: OHellPlayer, possibleBids: Option[Seq[Int]]): OHellPlayer = player.copy(possibleBids = possibleBids)

    override def getPossibleBids(player: OHellPlayer): Option[Seq[Int]] = player.possibleBids

    override def setTrick(player: OHellPlayer, tricksNumber: Option[Int]): OHellPlayer = player.copy(tricksNumber = tricksNumber)

    override def getTrick(player: OHellPlayer): Option[Int] = player.tricksNumber

    def setIsDealer(player: OHellPlayer, isDealer: Boolean): OHellPlayer = player.copy(isDealer = isDealer)

    def isDealer(player: OHellPlayer): Boolean = player.isDealer

    def setIsHaveMove(player: OHellPlayer, isHaveMove: Boolean): OHellPlayer = player.copy(isHaveMove = isHaveMove)

    def isHaveMove(player: OHellPlayer): Boolean = player.isHaveMove

    override def addScore(player: OHellPlayer, score: Int): OHellPlayer = player.copy(score = player.score + score)

    override def getScore(player: OHellPlayer): Int = player.score
  }
}

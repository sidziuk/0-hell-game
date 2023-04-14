package com.sidziuk

import scala.collection.immutable.SortedSet


case class Player(id: String,
                  name: String,
                  hand: Option[Set[Card]] = None,
                  bid: Option[Int] = None,
                  possibleBids: Option[Seq[Int]] = None,
                  tricksNumber: Option[Int] = None,
                  isDealer: Boolean = false,
                  isHaveMove: Boolean = false,
                  score: Int = 0)

trait PlayerAlgebra {
  def createPlayer(id: String, name: String): Player

  def addCardToPlayerHand(player: Player, card: Card): Player

  def playCardFromPlayerHand(player: Player, card: Card): (Player, Option[Card])

  def setBid(player: Player, bid: Option[Int]): Player

  def getBid(player: Player): Option[Int]

  def setPossibleBids(player: Player, possibleBids: Option[Seq[Int]]): Player

  def getPossibleBids(player: Player): Option[Seq[Int]]

  def setTrick(player: Player, trick: Option[Int]): Player

  def getTrick(player: Player): Option[Int]

  def setIsDealer(player: Player, isDealer: Boolean): Player

  def isDealer(player: Player): Boolean

  def setIsHaveMove(player: Player, isHaveMove: Boolean): Player

  def isHaveMove(player: Player): Boolean

  def addScore(player: Player, score: Int): Player

  def getScore(player: Player): Int

}

object PlayerAlgebra {
  //  def apply[F[_]](implicit F: PlayerAlgebra[F]): PlayerAlgebra[F] = F

  implicit val playerAlgebraSync: PlayerAlgebra = new PlayerAlgebra {

    override def createPlayer(id: String, name: String): Player = Player(id, name)

    override def addCardToPlayerHand(player: Player, card: Card): Player = player.copy(hand =
      player.hand match {
        case Some(hand) => Option(hand + card)
        case None => Option(SortedSet(card))
      }
    )

    override def playCardFromPlayerHand(player: Player, card: Card): (Player, Option[Card]) = player.hand match {
      case Some(hand) if hand.contains(card) => (player.copy(hand = {
        player.hand.map(_ - card) match {
          case Some(value) if value.isEmpty => None
          case hand@Some(_) => hand
        }
      }),
        Option(card))
      case None => (player, None)
    }

    override def setBid(player: Player, bid: Option[Int]): Player = player.copy(bid = bid)

    override def getBid(player: Player): Option[Int] = player.bid

    override def setPossibleBids(player: Player, possibleBids: Option[Seq[Int]]): Player = player.copy(possibleBids = possibleBids)

    override def getPossibleBids(player: Player): Option[Seq[Int]] = player.possibleBids

    override def setTrick(player: Player, tricksNumber: Option[Int]): Player = player.copy(tricksNumber = tricksNumber)

    override def getTrick(player: Player): Option[Int] = player.tricksNumber

    def setIsDealer(player: Player, isDealer: Boolean): Player = player.copy(isDealer = isDealer)

    def isDealer(player: Player): Boolean = player.isDealer

    def setIsHaveMove(player: Player, isHaveMove: Boolean): Player = player.copy(isHaveMove = isHaveMove)

    def isHaveMove(player: Player): Boolean = player.isHaveMove

    override def addScore(player: Player, score: Int): Player = player.copy(score = player.score + score)

    override def getScore(player: Player): Int = player.score
  }
}

//val m: PlayerAlgebra[IO] = PlayerAlgebra[IO]
//
//for {
// p <- m.createPlayer("gg", "ggrgr")
// p2 <- m.addCardToPlayerHand(p, (Card(Heart, Seven)))
//
//} yield ()

package com.sidziuk.domain.game

import com.sidziuk.domain.deck.{Card, Deck, DeckAlgebra, Suit}
import com.sidziuk.domain.game.GameRulesAlgebra.numberCardsOnHands
import io.circe.syntax.EncoderOps

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Random

case class OHellGame(
  deck: Option[Deck] = None,
  players: Seq[OHellPlayer],
  winner: Option[Seq[OHellPlayer]] = None,
  desk: Desk = Desk(),
  numberCardsOnHands: Int = numberCardsOnHands,
  scoreHistory: Map[UUID, Seq[Int]] = Map.empty,
  currentGameRound: Int = 1,
  moveType: MoveType = Bid,
  ifGameEnd: Boolean = false,
  isGameStarted: Boolean = false,
  minPlayerNumber: Int  = 2,
  maxPlayersNumber: Int = 7,
  deskWinner: Option[UUID] = None
) extends Game()

object OHellGame {

  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encodeUUID: Encoder[UUID] = Encoder.encodeString.contramap[UUID](_.toString)
  implicit val decodeUUID: Decoder[UUID] = Decoder.decodeString.emapTry { str =>
    scala.util.Try(UUID.fromString(str))
  }

  implicit val ohellGameEncoder: Encoder[OHellGame] = deriveEncoder[OHellGame]
  implicit val ohellGameDecoder: Decoder[OHellGame] = deriveDecoder[OHellGame]
}

trait GameRulesAlgebra {
  def startGame(game: OHellGame): OHellGame

  def makeMove(game: OHellGame, move: OHellMove): OHellGame
}

object GameRulesAlgebra {
  //  def apply[F[_]](implicit F: GameRulesAlgebra[F]): GameRulesAlgebra[F] = F

  Random.setSeed(5)

  val numberCardsOnHands = 3
  private val flipRoundNumber    = numberCardsOnHands
  private val maxRounds          = numberCardsOnHands * 2 - 1

  implicit def gameRulesAlgebraSync(implicit deckalg: DeckAlgebra, playeralg: PlayerAlgebra): GameRulesAlgebra =
    new GameRulesAlgebra {

      def nexPlayerIndex(currentPlayerIndex: Int, players: Seq[OHellPlayer]): Int = (currentPlayerIndex + 1) % players.size

      override def startGame(game: OHellGame): OHellGame = {

        @tailrec
        def cardgg(players: Seq[OHellPlayer], deck: Deck, cardNumber: Int): (Seq[OHellPlayer], Deck) = {

          @tailrec
          def getCard(deck: Deck, cards: Seq[Card] = Nil, cardNumber: Int = players.size): (Seq[Card], Deck) = {
            if (cardNumber == 0) (cards, deck)
            else {
              val (card, newDeck) = deckalg.deal(deck)
              getCard(newDeck, cards :+ card, cardNumber - 1)
            }
          }

          if (cardNumber == 0) (players, deck)
          else {
            val (cards, newDeck) = getCard(deck)
            val playersWithCards = players.zipWithIndex.map { case (player, index) =>
              playeralg.addCardToPlayerHand(player, cards(index))
            }
            cardgg(playersWithCards, newDeck, cardNumber - 1)
          }
        }

        if (!game.isGameStarted && game.players.size >= game.minPlayerNumber && game.players.size <= game.maxPlayersNumber) {
          val randomPlayer = Random.shuffle(game.players).head
          val dealerId = randomPlayer.uuid
          val movePlayerId = game.players(nexPlayerIndex(game.players.indexOf(randomPlayer), game.players)).uuid
          val (newplayers, deck): (Seq[OHellPlayer], Deck) =
            cardgg(game.players, deckalg.createShuffledDesk, numberCardsOnHands)
          val superPlayers = newplayers.map { player =>
            val mayBeDealerPlayer = if (player.uuid == dealerId) player.copy(isDealer = true) else player
            if (player.uuid == movePlayerId)
              mayBeDealerPlayer.copy(isHaveMove = true, possibleBids = Option(0 to numberCardsOnHands))
            else mayBeDealerPlayer
          }
          val deckWithTrump = Option(deckalg.getTrump(deck))
          val newMap: Map[UUID, Seq[Int]] = Map()
          val mapscore: Map[UUID, Seq[Int]] = game.players.foldLeft(newMap) { case (map, pl) => map + (pl.uuid -> Seq()) }
          OHellGame(deckWithTrump, superPlayers, scoreHistory = mapscore, isGameStarted = true)
        } else game
      }

      def checkIfMoveValid(game: OHellGame, move: OHellMove): Boolean =
        if (
          game.players.iterator.filter(_.isHaveMove).map(_.uuid).contains(move.playerId)
          && move.moveType == game.moveType
        ) {
          //        println("-----------")
          println(move)
          move match {
            case OHellMove(playerId, moveType, card, bid)
                if moveType == Bid &&
                  card.isEmpty &&
                  bid.nonEmpty &&
                  game.players.filter(_.uuid == playerId).head.possibleBids.get.contains(bid.get) =>
              true
            case OHellMove(playerId, moveType, card, bid)
                if moveType == PlayCard &&
                  card.nonEmpty &&
                  bid.isEmpty &&
                  game.players.filter(_.uuid == playerId).head.hand.get.contains(card.get) && {
                    game.desk.firstPlayer match {
                      case Some(value) =>
                        card.get.suit == value._2 ||
                        (card.get.suit != value._2 && game.players
                          .filter(_.uuid == playerId)
                          .head
                          .hand
                          .get
                          .exists(_.suit != value._2))
                      case None        => true
                    }
                  } =>
              true
            case _ => false
          }
        } else false

      override def makeMove(game: OHellGame, move: OHellMove): OHellGame =
        if (!game.ifGameEnd && checkIfMoveValid(game, move)) {
          move match {
            case OHellMove(playerId, moveType, _, bid) if moveType == Bid =>
              val players                      = game.players
              val currentPlayer                = players.filter(_.uuid == playerId).head
              val currentPlayerIndex           = players.indexOf(currentPlayer)
              val nextPlayerIndex              = nexPlayerIndex(currentPlayerIndex, players)
              val newMoveType                  = if (currentPlayer.isDealer) PlayCard else Bid
              val newPlayers: Seq[OHellPlayer] =
                if (currentPlayer.isDealer) players.zipWithIndex.map { case (player, index) =>
                  if (player.isDealer) player.copy(bid = bid, possibleBids = None, isHaveMove = false)
                  else if (index == nextPlayerIndex) player.copy(isHaveMove = true)
                  else player
                }
                else {
                  players.zipWithIndex.map { case (player, index) =>
                    if (player.uuid == playerId) {
                      player.copy(bid = bid, possibleBids = None, isHaveMove = false)
                    } else if (index == nextPlayerIndex) {
                      if (player.isDealer) {
                        val sumOfBidsOfBeforePlayers = players.filter(_.bid.nonEmpty).map(_.bid.get).sum + bid.get
                        player.copy(
                          possibleBids = Option(
                            (0 to game.numberCardsOnHands)
                              .filter { number =>
                                if (sumOfBidsOfBeforePlayers > game.numberCardsOnHands) true
                                else (game.numberCardsOnHands - sumOfBidsOfBeforePlayers) != number
                              }
                          ),
                          isHaveMove = true
                        )
                      } else player.copy(possibleBids = Option(0 to game.numberCardsOnHands), isHaveMove = true)
                    } else player
                  }
                }
              game.copy(players = newPlayers, moveType = newMoveType, deskWinner = None)

            case OHellMove(playerId, moveType, card, _) if moveType == PlayCard =>
              val players            = game.players
              val currentPlayer      = players.filter(_.uuid == playerId).head
              val currentDealerIndex = players.indexOf(players.filter(_.isDealer).head)
              val currentPlayerIndex = players.indexOf(currentPlayer)
              val nextPlayerIndex    = nexPlayerIndex(currentPlayerIndex, players)
              val newCardsOnDesk     = game.desk.cards match {
                case Some(cards) => Desk(Option(cards + (playerId -> card.get)), game.desk.firstPlayer)
                case None        => Desk(Option(Map(playerId -> card.get)), Option(currentPlayer.uuid, card.get.suit))
              }
              if (newCardsOnDesk.cards.get.size == players.size) {

                def getWinnerFromCardsOnDesk(cardsOnDesk: Desk, trumpSuit: Suit): UUID = {
                  val trumpCards = cardsOnDesk.cards.get.filter(_._2.suit == trumpSuit)
                  if (trumpCards.nonEmpty) {
                    val (playerId, _) = trumpCards.maxBy(_._2.rank)
                    playerId
                  } else {
                    val leadCards     = cardsOnDesk.cards.get.filter(_._2.suit == cardsOnDesk.firstPlayer.get._2)
                    val (playerId, _) = leadCards.maxBy(_._2.rank)
                    playerId
                  }
                }

                def addTrick(beforeTrick: Option[Int]): Option[Int] = beforeTrick match {
                  case Some(value) => Option(value + 1)
                  case None        => Option(1)
                }

                val deskWinner = getWinnerFromCardsOnDesk(newCardsOnDesk, game.deck.get.trump.get.suit)
                //              println(deskWinner)

                val newPlayers = players.map { pl =>
                  val pl1 = if (pl.uuid == deskWinner) {
                    pl.copy(tricksNumber = addTrick(pl.tricksNumber))
                  } else pl
                  if (pl1.uuid == playerId) {
                    playeralg.playCardFromPlayerHand(pl1, card.get)._1.copy(isHaveMove = pl.uuid == deskWinner)
                  } else
                    pl1.copy(isHaveMove = pl1.uuid == deskWinner)
                }
                if (newPlayers.forall(_.hand.isEmpty)) {

                  @tailrec
                  def cardgg(
                    players: Seq[OHellPlayer],
                    deck: Deck,
                    cardNumberOnHands: Int
                  ): (Seq[OHellPlayer], Deck) = {

                    @tailrec
                    def getCard(
                      deck: Deck,
                      cards: Seq[Card] = Nil,
                      cardNumber: Int = players.size
                    ): (Seq[Card], Deck) = {
                      if (cardNumber == 0) (cards, deck)
                      else {
                        val (card, newDeck) = deckalg.deal(deck)
                        getCard(newDeck, cards :+ card, cardNumber - 1)
                      }
                    }

                    if (cardNumberOnHands == 0) (players, deck)
                    else {
                      val (cards, newDeck) = getCard(deck)
                      val playersWithCards = players.zipWithIndex.map { case (player, index) =>
                        playeralg.addCardToPlayerHand(player, cards(index))
                      }
                      cardgg(playersWithCards, newDeck, cardNumberOnHands - 1)
                    }
                  }

                  val newPlayersWithScore = newPlayers.map { player =>
                    val score1 = if (player.tricksNumber.getOrElse(0) == player.bid.get) 10 else 0
                    val score2 = player.tricksNumber.getOrElse(0)
                    val score3 = player.score

                    //                  println(s"$score1 + $score2 + $score3")
                    player.copy(score = score1 + score2 + score3)
                  }
                  val newScoreHistory     = game.scoreHistory.map { case (key, value) =>
                    val sc1                 = newPlayersWithScore.filter(_.uuid == key).head.score
                    val sc2                 = if (value.nonEmpty) value.sum else 0
                    val newPlayerScore: Int = sc1 - sc2
                    (key, value :+ newPlayerScore)
                  }

                  if (game.currentGameRound >= maxRounds) {

                    val maxWinner                         = newPlayersWithScore.maxBy(pl => pl.score)
                    val winners: Option[Seq[OHellPlayer]] = Option(newPlayersWithScore.filter(_ == maxWinner))

                    game.copy(
                      players = newPlayersWithScore,
                      winner = winners,
                      ifGameEnd = true,
                      desk = Desk(),
                      scoreHistory = newScoreHistory,
                      deskWinner = Option(deskWinner)
                    )
                  } else {
                    val newCardNumberOnHands =
                      if (game.currentGameRound < flipRoundNumber) game.numberCardsOnHands - 1
                      else game.numberCardsOnHands + 1
                    val newDeck              = deckalg.createShuffledDesk
                    val newDekWithTrump      = deckalg.getTrump(newDeck)
                    val newMoveType          = Bid
                    val newCurrentGameRound  = game.currentGameRound + 1

                    val (newPlayersWithCards, newDekWithTrump2) =
                      cardgg(newPlayersWithScore, newDekWithTrump, newCardNumberOnHands)

                    val newPlayers2 = newPlayersWithCards.zipWithIndex.map { case (pl, index) =>
                      //                    println("currentDealerIndex" + currentDealerIndex)
                      if (index == nexPlayerIndex(currentDealerIndex, newPlayersWithCards)) {
                        //                    println("--------------")
                        pl.copy(
                          tricksNumber = None,
                          bid = None,
                          possibleBids = None,
                          isDealer = true,
                          isHaveMove = false
                        )
                      } else if (
                        index == nexPlayerIndex(
                          nexPlayerIndex(currentDealerIndex, newPlayersWithCards),
                          newPlayersWithCards
                        )
                      ) {
                        pl.copy(
                          tricksNumber = None,
                          bid = None,
                          possibleBids = Option(0 to newCardNumberOnHands),
                          isDealer = false,
                          isHaveMove = true
                        )
                      } else {
                        pl.copy(
                          tricksNumber = None,
                          bid = None,
                          possibleBids = None,
                          isDealer = false,
                          isHaveMove = false
                        )
                      }
                    }
                    game.copy(
                      deck = Option(newDekWithTrump2),
                      players = newPlayers2,
                      numberCardsOnHands = newCardNumberOnHands,
                      scoreHistory = newScoreHistory,
                      currentGameRound = newCurrentGameRound,
                      moveType = newMoveType,
                      desk = Desk(),
                      deskWinner = Option(deskWinner)
                    )
                  }
                } else game.copy(players = newPlayers, desk = Desk(), deskWinner = Option(deskWinner))
              } else {
                val newPlayers = players.zipWithIndex.map { case (player, index) =>
                  if (playerId == player.uuid) {
                    val (playerWithoutCard, _) = playeralg.playCardFromPlayerHand(player, card.get)
                    playerWithoutCard.copy(isHaveMove = false)
                  } else if (index == nextPlayerIndex) {
                    player.copy(isHaveMove = true)
                  } else player
                }
                game.copy(players = newPlayers, desk = newCardsOnDesk, deskWinner = None)
              }
            case _                                                         => game
          }
        } else game
    }
}



//object d extends App {
//
//  val moves = Seq(
//    Move(UUID.fromString("3"), Bid, None, Option(3)),
//    Move("1", Bid, None, Option(1)),
//    Move("2", Bid, None, Option(0)),
//    Move("3", PlayCard, Option(Card(Club, Ten)), None),
//    Move("1", PlayCard, Option(Card(Spade, King)), None),
//    Move("2", PlayCard, Option(Card(Club, Ace)), None)
//  )
//
//
//  val oo = GameRulesAlgebra.gameRulesAlgebraSync.createNewGame(Seq(OHellPlayer("1", "Vasa"), OHellPlayer("2", "Hoha"), OHellPlayer("3", "Vasa")))
//  println(oo.players)
//  println(oo.moveType)
//
//  val ll = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(oo, Move("3", Bid, None, Option(3)))
//  println(ll.players)
//  println(ll.moveType)
//
//  val ll1 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll, Move("1", Bid, None, Option(1)))
//  println(ll1.players)
//  println(ll.moveType)
//
//  val ll3 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll1, Move("2", Bid, None, Option(0)))
//  println(ll3.players)
//  println(ll3.moveType)
//
//  println("kozer " + ll3.deck.get.trump.get.suit)
//
//  val ll4 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll3, Move("3", PlayCard, Option(Card(Club, Ten)), None))
//  println(ll4.players)
//  println(ll4.moveType)
//  println(ll4.desk)
//
//  val ll5 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll4, Move("1", PlayCard, Option(Card(Spade, King)), None))
//  println(ll5.players)
//  println(ll5.moveType)
//  println(ll5.desk)
//
//  val ll6 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll5, Move("2", PlayCard, Option(Card(Club, Ace)), None))
//  println(ll6.players)
//  println(ll6.moveType)
//  println(ll6.desk)
//
//  val ll7 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll6, Move("2", PlayCard, Option(Card(Spade, Four)), None))
//  println(ll7.players)
//  println(ll7.moveType)
//  println(ll7.desk)
//
//  val ll8 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll7, Move("3", PlayCard, Option(Card(Spade, Ace)), None))
//  println(ll8.players)
//  println(ll8.moveType)
//  println(ll8.desk)
//
//  val ll9 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll8, Move("1", PlayCard, Option(Card(Spade, Six)), None))
//  println(ll9.players)
//  println(ll9.moveType)
//  println(ll9.desk)
//
//  val l20 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(ll9, Move("3", PlayCard, Option(Card(Spade, Two)), None))
//  println(l20.players)
//  println(l20.moveType)
//  println(l20.desk)
//
//  val l21 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l20, Move("1", PlayCard, Option(Card(Diamond, Ace)), None))
//  println(l21.players)
//  println(l21.moveType)
//  println(l21.desk)
//
//  val l22 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l21, Move("2", PlayCard, Option(Card(Heart, Four)), None))
//  println(l22.players)
//  println(l22.moveType)
//  println(l22.desk)
//
//  val llq1 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l22, Move("1", Bid, None, Option(0)))
//  println(llq1.players)
//  println(llq1.moveType)
//
//  val llq2 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(llq1, Move("2", Bid, None, Option(0)))
//  println(llq2.players)
//  println(llq2.moveType)
//
//  val llq3 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(llq2, Move("3", Bid, None, Option(1)))
//  println(llq3.players)
//  println(llq3.moveType)
//
//  println("kozer " + llq3.deck.get.trump.get.suit)
//
//  val l23 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(llq3, Move("1", PlayCard, Option(Card(Club, Ace)), None))
//  println(l23.players)
//  println(l23.moveType)
//  println(l23.desk)
//
//  val l24 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l23, Move("2", PlayCard, Option(Card(Club, Ten)), None))
//  println(l24.players)
//  println(l24.moveType)
//  println(l24.desk)
//
//  val l25 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l24, Move("3", PlayCard, Option(Card(Spade, King)), None))
//  println(l25.players)
//  println(l25.moveType)
//  println(l25.desk)
//
//  val l30 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l25, Move("3", PlayCard, Option(Card(Diamond, Ace)), None))
//  println(l30.players)
//  println(l30.moveType)
//  println(l30.desk)
//
//  val l31 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l30, Move("1", PlayCard, Option(Card(Heart, Four)), None))
//  println(l31.players)
//  println(l31.moveType)
//  println(l31.desk)
//
//  val l32 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l31, Move("2", PlayCard, Option(Card(Spade, Two)), None))
//  println(l32.players)
//  println(l32.moveType)
//  println(l32.desk)
//  println(l32.currentGameRound, l32.scoreHistory)
//
//  val bid1 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l32, Move("2", Bid, None, Option(0)))
//  println(bid1.players)
//  println(bid1.moveType)
//
//  val bid2 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid1, Move("3", Bid, None, Option(0)))
//  println(bid2.players)
//  println(bid2.moveType)
//
//  val bid3 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid2, Move("1", Bid, None, Option(0)))
//  println(bid3.players)
//  println(bid3.moveType)
//
//  println("kozer " + bid3.deck.get.trump.get.suit)
//
//  val l35 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid3, Move("2", PlayCard, Option(Card(Club, Ten)), None))
//  println(l35.players)
//  println(l35.moveType)
//  println(l35.desk)
//
//  val l36 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l35, Move("3", PlayCard, Option(Card(Diamond, Ace)), None))
//  println(l36.players)
//  println(l36.moveType)
//  println(l36.desk)
//
//  val l37 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l36, Move("1", PlayCard, Option(Card(Heart, Four)), None))
//  println(l37.players)
//  println(l37.moveType)
//  println(l37.desk)
//  println(l37.currentGameRound, l37.scoreHistory)
//
//  val bid4 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l37, Move("3", Bid, None, Option(1)))
//  println(bid4.players)
//  println(bid4.moveType)
//
//  val bid5 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid4, Move("1", Bid, None, Option(1)))
//  println(bid5.players)
//  println(bid5.moveType)
//
//  val bid6 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid5, Move("2", Bid, None, Option(1)))
//  println(bid6.players)
//  println(bid6.moveType)
//
//  println("kozer " + bid6.deck.get.trump.get.suit)
//
//  val l38 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid6, Move("3", PlayCard, Option(Card(Diamond,Ace)), None))
//  println(l38.players)
//  println(l38.moveType)
//  println(l38.desk)
//
//  val l39 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l38, Move("1", PlayCard, Option(Card(Heart,Four)), None))
//  println(l39.players)
//  println(l39.moveType)
//  println(l39.desk)
//
//  val l40 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l39, Move("2", PlayCard, Option(Card(Spade,Two)), None))
//  println(l40.players)
//  println(l40.moveType)
//  println(l40.desk)
//
//  val l41 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l40, Move("2", PlayCard, Option(Card(Club, Ten)), None))
//  println(l41.players)
//  println(l41.moveType)
//  println(l41.desk)
//
//  val l42 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l41, Move("3", PlayCard, Option(Card(Spade,King)), None))
//  println(l42.players)
//  println(l42.moveType)
//  println(l42.desk)
//
//  val l43 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l42, Move("1", PlayCard, Option(Card(Club,Ace)), None))
//  println(l43.players)
//  println(l43.moveType)
//  println(l43.desk)
//  println(l43.currentGameRound, l43.scoreHistory)
//
//  val bid31 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l43, Move("1", Bid, None, Option(0)))
//  println(bid31.players)
//  println(bid31.moveType)
//
//  val bid32 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid31, Move("2", Bid, None, Option(1)))
//  println(bid32.players)
//  println(bid32.moveType)
//
//  val bid33 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid32, Move("3", Bid, None, Option(1)))
//  println(bid33.players)
//  println(bid33.moveType)
//
//  println("kozer " + bid33.deck.get.trump.get.suit)
//
//  val l45 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(bid33, Move("1", PlayCard, Option(Card(Heart,Four)), None))
//  println(l45.players)
//  println(l45.moveType)
//  println(l45.desk)
//
//  val l46 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l45, Move("2", PlayCard, Option(Card(Spade,Ace)), None))
//  println(l42.players)
//  println(l46.moveType)
//  println(l46.desk)
//
//  val l47 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l46, Move("3", PlayCard, Option(Card(Heart,Nine)), None))
//  println(l47.players)
//  println(l47.moveType)
//  println(l47.desk)
//
//  val l48 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l47, Move("2", PlayCard, Option(Card(Spade,Two)), None))
//  println(l48.players)
//  println(l48.moveType)
//  println(l48.desk)
//
//  val l49 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l48, Move("3", PlayCard, Option(Card(Spade,King)), None))
//  println(l49.players)
//  println(l49.moveType)
//  println(l49.desk)
//
//  val l50 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l49, Move("1", PlayCard, Option(Card(Spade,Four)), None))
//  println(l50.players)
//  println(l50.moveType)
//  println(l50.desk)
//
//  val l51 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l50, Move("3", PlayCard, Option(Card(Diamond,Ace)), None))
//  println(l51.players)
//  println(l51.moveType)
//  println(l51.desk)
//
//  val l52 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l51, Move("1", PlayCard, Option(Card(Club,Ace)), None))
//  println(l52.players)
//  println(l52.moveType)
//  println(l52.desk)
//
//  val l53 = GameRulesAlgebra.gameRulesAlgebraSync.makeMove(l52, Move("2", PlayCard, Option(Card(Club,Ten)), None))
//  println(l53.players)
//  println(l53.moveType)
//  println(l53.desk)
//
//  println(l53.currentGameRound, l53.scoreHistory, l53.ifGameEnd, l53.winner)
//
//
//
//}

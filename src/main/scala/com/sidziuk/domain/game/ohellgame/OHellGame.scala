package com.sidziuk.domain.game.ohellgame

import com.sidziuk.domain.Game
import com.sidziuk.domain.game.Desk
import com.sidziuk.domain.game.deck.{Card, Deck, DeckAlgebra, Suit}
import com.sidziuk.domain.game.ohellgame.GameRulesAlgebra.numberCardsOnHands

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
                      if (index == nexPlayerIndex(currentDealerIndex, newPlayersWithCards)) {
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
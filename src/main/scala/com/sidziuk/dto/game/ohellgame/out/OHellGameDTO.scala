package com.sidziuk.dto.game.ohellgame.out

import com.sidziuk.domain.game.Desk
import com.sidziuk.domain.game.deck.Card
import com.sidziuk.domain.game.ohellgame.{MoveType, OHellPlayer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class OHellGameDTO(
    cardNumberInDeck: Int,
    trump: Card,
    players: Seq[OHellPlayerDTO],
    winner: Option[Seq[OHellPlayer]],
    desk: Desk,
    numberCardsOnHands: Int,
    scoreHistory: Map[UUID, Seq[Int]],
    currentGameRound: Int,
    moveType: MoveType,
    ifGameEnd: Boolean,
    isGameStarted: Boolean,
    minPlayerNumber: Int,
    maxPlayersNumber: Int,
    deskWinner: Option[UUID]
)

object OHellGameDTO {
  implicit val Encoder: Encoder[OHellGameDTO] = deriveEncoder[OHellGameDTO]
  implicit val Decoder: Decoder[OHellGameDTO] = deriveDecoder[OHellGameDTO]
}

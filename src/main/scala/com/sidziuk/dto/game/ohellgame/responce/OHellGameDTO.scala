package com.sidziuk.dto.game.ohellgame.responce

import com.sidziuk.deck.{Card, Deck}
import com.sidziuk.domain.game.{Bid, Desk, MoveType, OHellPlayer}
import com.sidziuk.domain.game.GameRulesAlgebra.numberCardsOnHands
import com.sidziuk.dto.game.ohellgame.command.OHellMoveDTO
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import java.util.UUID

case class OHellGameDTO(
                      cardNumberInDeck: Int,
                      trump: Card,
                      player: Seq[OHellPlayerDTO],
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
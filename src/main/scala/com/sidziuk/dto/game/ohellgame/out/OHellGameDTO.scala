package com.sidziuk.dto.game.ohellgame.out

import com.sidziuk.domain.deck.Card
import com.sidziuk.domain.game.{Desk, MoveType, OHellPlayer}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

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
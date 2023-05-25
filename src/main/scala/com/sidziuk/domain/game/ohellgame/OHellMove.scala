package com.sidziuk.domain.game.ohellgame

import com.sidziuk.domain.game.deck.Card
import com.sidziuk.dto.WebSocketDTO
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.util.UUID

case class OHellMove(playerId: UUID, moveType: MoveType, card: Option[Card], bid: Option[Int]) extends WebSocketDTO
object OHellMove {
  implicit val moveEncoder: Encoder[OHellMove] = deriveEncoder[OHellMove]
  implicit val moveDecoder: Decoder[OHellMove] = deriveDecoder[OHellMove]
}

sealed trait MoveType
case object Bid extends MoveType
case object PlayCard extends MoveType

object MoveType {
  implicit val encoder: Encoder[MoveType] = Encoder.encodeString.contramap[MoveType](_.toString)
  implicit val decoder: Decoder[MoveType] = Decoder.decodeString.emap[MoveType] {
    case "Bid"      => Right(Bid)
    case "PlayCard" => Right(PlayCard)
    case other      => Left(s"Unknown MoveType: $other")
  }
}

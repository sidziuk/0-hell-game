package com.sidziuk

import com.sidziuk.Rank.Nine
import com.sidziuk.Suit.Heart
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.generic.auto._

import java.util.UUID

case class Move(playerId: UUID, moveType: MoveType, card: Option[Card], bid: Option[Int])
object Move {
  implicit val encoder: Encoder[Move] = deriveEncoder[Move]
  implicit val decoder: Decoder[Move] = deriveDecoder[Move]
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

object app1 extends App {
  val f = Move(UUID.randomUUID(), Bid, Option(Card(Heart, Nine)), Option(2))
  println(f.asJson)
}

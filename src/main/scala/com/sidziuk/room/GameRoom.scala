package com.sidziuk.room

import com.sidziuk.game.{Game, OHellGame}
import com.sidziuk.player.OHellPlayer
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

import java.util.UUID
case class GameRoom(roomId: UUID, game: Game)

object GameRoom {

  implicit val gameDecoder: Decoder[Game] = {
    Decoder[OHellGame].map[Game](identity)
  }

  implicit val gameEncoder: Encoder[Game] = {
    Encoder[OHellGame].asInstanceOf[Encoder[Game]]
  }

  implicit val gameRoomDecoder: Decoder[GameRoom] = deriveDecoder[GameRoom]
  implicit val gameRoomEncoder: Encoder[GameRoom] = deriveEncoder[GameRoom]

}


//object GameRoom {
//  implicit val gameEncoder: Encoder[Game] = Encoder.instance {
//    case ohellGame: OHellGame => ohellGame.asJson
//  }
//
//  implicit val gameDecoder: Decoder[Game] = Decoder.instance { cursor =>
//    cursor.as[OHellGame].map(identity: Game)
//  }
//
//  implicit val encoder: Encoder[GameRoom] = deriveEncoder[GameRoom]
//  implicit val decoder: Decoder[GameRoom] = deriveDecoder[GameRoom]
//}

object app134 extends App {
  val f = OHellGame(players = Seq(OHellPlayer(uuid = UUID.randomUUID(), name = "Vasa")))
  val ff = GameRoom(UUID.randomUUID(), f)
  println(ff.asJson)
}
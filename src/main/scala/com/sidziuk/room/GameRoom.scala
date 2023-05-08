package com.sidziuk.room

import cats.effect.IO
import com.sidziuk.domain.game.{Game, OHellGame, OHellPlayer}
import fs2.concurrent.Topic
import io.circe.generic.JsonCodec
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}

import java.util.UUID
case class GameRoom[F[_]](roomUUID: UUID, gameTopic: Option[Topic[F, String]] = None, game: Game)
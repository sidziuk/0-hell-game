package com.sidziuk.domain.room

import com.sidziuk.domain.game.Game
import fs2.concurrent.Topic

import java.util.UUID
case class GameRoom[F[_]](roomUUID: UUID, gameTopic: Option[Topic[F, String]] = None, game: Game)
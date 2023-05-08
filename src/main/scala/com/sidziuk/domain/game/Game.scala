package com.sidziuk.domain.game

import com.sidziuk.domain.Player

trait Game {
  val minPlayerNumber: Int
  val maxPlayersNumber: Int
  val isGameStarted: Boolean
  val players: Seq[Player]
  val winner: Option[Seq[Player]]
}

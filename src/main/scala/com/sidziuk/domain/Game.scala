package com.sidziuk.domain

trait Game {
  val minPlayerNumber: Int
  val maxPlayersNumber: Int
  val isGameStarted: Boolean
  val players: Seq[Player]
  val winner: Option[Seq[Player]]
}

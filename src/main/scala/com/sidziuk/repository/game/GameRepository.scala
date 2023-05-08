package com.sidziuk.repository.game

import com.sidziuk.domain.player.RegisteredPlayer

import java.util.UUID

trait GameRepository[F[_]] {
  def getAllPlayers(): F[List[RegisteredPlayer]]

}
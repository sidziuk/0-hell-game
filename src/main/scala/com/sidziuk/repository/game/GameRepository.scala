package com.sidziuk.repository.game

import com.sidziuk.domain.player.RegisteredPlayer

trait GameRepository[F[_]] {
  def getAllPlayers(): F[List[RegisteredPlayer]]

}
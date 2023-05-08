package com.sidziuk.servis.game

import com.sidziuk.domain.player.RegisteredPlayer

import java.util.UUID

trait GameService[F[_]] {
  def getAllPlayers(): F[Map[UUID, RegisteredPlayer]]

}

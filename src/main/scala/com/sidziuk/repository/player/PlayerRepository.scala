package com.sidziuk.repository.player

import com.sidziuk.domain.player.RegisteredPlayer

trait PlayerRepository[F[_]] {
  def create(playerUUID: String, name: String): F[Int]

  def get(name: String): F[Option[String]]

  def remove(playerUUID: String): F[Int]

  def getAllPlayers(): F[List[RegisteredPlayer]]

}
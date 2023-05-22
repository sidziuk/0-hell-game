package com.sidziuk.repository.player

import com.sidziuk.domain.player.RegisteredPlayer

trait PlayerRepository[F[_]] {
  def create(playerUUID: String, name: String, password: String): F[Int]

  def get(name: String, password: String): F[Option[String]]

  def remove(playerUUID: String): F[Int]

  def getAllPlayers(): F[List[RegisteredPlayer]]

}
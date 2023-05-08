package com.sidziuk.servis.player

import com.sidziuk.dto.player.CreatePlayerDTO

trait PlayerService[F[_]] {
  def create(createdPlayer: CreatePlayerDTO): F[Either[String, String]]

  def get(name: String, password: String): F[Option[String]]

  def delete(playerUUID: String): F[Option[String]]

}

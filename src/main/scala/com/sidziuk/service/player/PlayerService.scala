package com.sidziuk.service.player

import com.sidziuk.domain.player.RegisteredPlayer
import com.sidziuk.dto.player.in.CreatePlayerDTO
import com.sidziuk.dto.player.out.PlayerUUID

import java.util.UUID

trait PlayerService[F[_]] {
  def create(createdPlayer: CreatePlayerDTO): F[Either[String, PlayerUUID]]

  def get(name: String): F[Either[String, PlayerUUID]]

  def delete(playerUUID: String): F[Either[String, PlayerUUID]]

  def getAllPlayers(): F[Map[UUID, RegisteredPlayer]]

}

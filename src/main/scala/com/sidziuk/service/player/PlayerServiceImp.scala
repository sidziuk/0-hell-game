package com.sidziuk.service.player

import cats.effect.Sync
import cats.implicits._
import com.sidziuk.domain.player.RegisteredPlayer
import com.sidziuk.dto.player.in.CreatePlayerDTO
import com.sidziuk.dto.player.out.PlayerUUID
import com.sidziuk.repository.player.PlayerRepository

import java.util.UUID
class PlayerServiceImp[F[_]: Sync](playerRepository: PlayerRepository[F])
    extends PlayerService[F] {

  override def create(
      createdPlayer: CreatePlayerDTO
  ): F[Either[String, PlayerUUID]] = {
    val playerId = UUID.randomUUID().toString
    playerRepository
      .create(playerId, createdPlayer.name, createdPlayer.password)
      .flatMap { x =>
        if (x != 0) Sync[F].delay(Right(PlayerUUID(playerId)))
        else
          Sync[F].delay(
            Left(s"Failed to create player with name ${createdPlayer.name}")
          )
      }
  }

  override def get(
      name: String,
      password: String
  ): F[Either[String, PlayerUUID]] = playerRepository
    .get(name, password)
    .flatMap {
      case Some(playerUUID) => Sync[F].delay(Right(PlayerUUID(playerUUID)))
      case None             => Sync[F].delay(Left(s"Failed to get player with name $name"))
    }

  override def delete(playerUUID: String): F[Either[String, PlayerUUID]] =
    playerRepository.remove(playerUUID).flatMap { x =>
      if (x == 0)
        Sync[F].delay(Left(s"Failed to delete player with id $playerUUID"))
      else Sync[F].delay(Right(PlayerUUID(playerUUID)))
    }

  override def getAllPlayers(): F[Map[UUID, RegisteredPlayer]] = {
    playerRepository.getAllPlayers().map(_.map(player => player.uuid -> player).toMap)
  }
}

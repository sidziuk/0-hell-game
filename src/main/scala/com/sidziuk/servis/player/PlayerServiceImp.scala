package com.sidziuk.servis.player

import cats.syntax.functor._
import cats.effect.{Async, ExitCode, IO, IOApp, Sync}
import cats.implicits._
import com.sidziuk.domain.game.{GameRulesAlgebra, MoveType, OHellGame, OHellMove, OHellPlayer}
import com.sidziuk.domain.player.RegisteredPlayer
import com.sidziuk.dto.player.CreatePlayerDTO
import com.sidziuk.repository.player.PlayerRepository
import doobie.WeakAsync.doobieWeakAsyncForAsync

import java.util.UUID
class PlayerServiceImp[F[_]: Sync](playerRepository: PlayerRepository[F]) extends PlayerService[F] {

  override def create(createdPlayer: CreatePlayerDTO): F[Either[String, String]] = {
    val playerId = UUID.randomUUID().toString
    playerRepository
      .create(playerId, createdPlayer.name, createdPlayer.password)
      .flatMap { x =>
        if (x != 0) Sync[F].delay(Right(playerId))
        else Sync[F].delay(Left(s"Failed to create player with name ${createdPlayer.name}"))
      }
  }

  override def get(name: String, password: String): F[Option[String]] = playerRepository.get(name, password)

  override def delete(playerUUID: String): F[Option[String]] =
    playerRepository.remove(playerUUID).flatMap{ x =>
      if (x == 0) Sync[F].delay(None) else Sync[F].delay(Option(playerUUID))
    }
}

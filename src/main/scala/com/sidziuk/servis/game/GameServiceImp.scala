package com.sidziuk.servis.game

import cats.effect.Sync
import cats.implicits._
import com.sidziuk.domain.player.RegisteredPlayer
import com.sidziuk.dto.player.CreatePlayerDTO
import com.sidziuk.repository.game.GameRepository
import com.sidziuk.repository.player.PlayerRepository

import java.util.UUID
class GameServiceImp[F[_]: Sync](gameRepository: GameRepository[F]) extends GameService[F] {

  override def getAllPlayers(): F[Map[UUID, RegisteredPlayer]] = {
    gameRepository.getAllPlayers().map(_.map(player => player.uuid -> player).toMap)
  }

}

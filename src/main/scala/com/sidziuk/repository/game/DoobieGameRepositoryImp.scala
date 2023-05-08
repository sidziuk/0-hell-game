package com.sidziuk.repository.game

import cats.effect.Sync
import com.sidziuk.domain.player.RegisteredPlayer
import doobie._
import doobie.implicits._

import java.util.UUID

class DoobieGameRepositoryImp[F[_]: Sync](transactor: Transactor[F]) extends GameRepository[F] {

  override def getAllPlayers(): F[List[RegisteredPlayer]] = {
    sql"""
    SELECT id, name, password
    FROM player
  """.query[(String, String, String)]
      .map { case (id, name, password) => RegisteredPlayer(UUID.fromString(id), name, password) }
      .to[List]
      .transact(transactor)
  }
}

object DoobieGameRepositoryImp {
  def apply[F[_]: Sync](transactor: Transactor[F]): DoobieGameRepositoryImp[F] =
    new DoobieGameRepositoryImp[F](transactor)

}

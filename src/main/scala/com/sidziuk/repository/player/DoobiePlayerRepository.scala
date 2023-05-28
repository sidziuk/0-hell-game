package com.sidziuk.repository.player

import cats.effect.Sync
import cats.syntax.all._
import com.sidziuk.domain.player.RegisteredPlayer
import doobie._
import doobie.implicits._

import java.util.UUID

class DoobiePlayerRepository[F[_]: Sync](transactor: Transactor[F]) extends PlayerRepository[F] {

  override def create(playerUUID: String, name: String): F[Int] = {
    for {
      existingPlayer <- get(name)
      result         <- existingPlayer match {
                          case Some(_) =>
                            Sync[F].pure(0)
                          case None    =>
                            sql"""
                            INSERT INTO player (id, name, password)
                            VALUES ($playerUUID, $name)
                            """.update.run
                              .transact(transactor)
                        }
    } yield result
  }

  override def get(name: String): F[Option[String]] =
    sql"""
    SELECT id
    FROM player
    WHERE name = $name
    """
      .query[String]
      .option
      .transact(transactor)

  override def remove(playerUUID: String): F[Int] =
    sql"""
    DELETE FROM player
    WHERE id = $playerUUID
    """.update.run
      .transact(transactor)

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

object DoobiePlayerRepository {
  def apply[F[_]: Sync](transactor: Transactor[F]): DoobiePlayerRepository[F] =
    new DoobiePlayerRepository[F](transactor)

}

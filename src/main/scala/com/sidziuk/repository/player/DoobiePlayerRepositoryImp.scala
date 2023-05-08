package com.sidziuk.repository.player

import cats.{FlatMap, Monad}
import cats.effect.{Async, MonadCancelThrow, Resource, Sync}
import doobie.WeakAsync.doobieWeakAsyncForAsync
import doobie._
import doobie.implicits._
import sun.security.util.Password
import cats.instances._
import cats.syntax.all._
import com.sidziuk.dto.player.CreatePlayerDTO
import org.h2.jdbcx.JdbcConnectionPool

import java.util.UUID

class DoobiePlayerRepositoryImp[F[_]: Sync](transactor: Transactor[F]) extends PlayerRepository[F] {

  override def create(playerUUID: String, name: String, password: String): F[Int] = {
    for {
      existingPlayer <- get(name, password)
      result         <- existingPlayer match {
                          case Some(_) =>
                            Sync[F].pure(0)
                          case None    =>
                            sql"""
                            INSERT INTO player (id, name, password)
                            VALUES ($playerUUID, $name, $password)
                            """.update.run
                              .transact(transactor)
                        }
    } yield result
  }

  override def get(name: String, password: String): F[Option[String]] =
    sql"""
    SELECT id
    FROM player
    WHERE name = $name AND password = $password
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
}

object DoobiePlayerRepositoryImp {
  def apply[F[_]: Sync](transactor: Transactor[F]): DoobiePlayerRepositoryImp[F] =
    new DoobiePlayerRepositoryImp[F](transactor)

}

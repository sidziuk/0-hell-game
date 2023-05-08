package com.sidziuk.repository.player

trait PlayerRepository[F[_]] {
  def create(playerUUID: String, name: String, password: String): F[Int]

  def get(name: String, password: String): F[Option[String]]

  def remove(playerUUID: String): F[Int]
}
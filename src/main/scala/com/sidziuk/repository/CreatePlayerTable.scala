package com.sidziuk.repository

import cats.effect._
import cats.implicits.toFunctorOps
import doobie._
import doobie.implicits._

object CreatePlayerTable {
  def apply[F[_]: Async](tr: Transactor[F]): F[Unit] =
    sql"""
   CREATE TABLE IF NOT EXISTS player (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
  )
  """.update
      .run
      .transact(tr)
      .void

}

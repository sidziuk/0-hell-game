package com.sidziuk.service.room

import com.sidziuk.domain.player.RegisteredPlayer

import java.util.UUID

trait RoomServiceHelper[F[_]] {

  def createNewRoom(gameType: String, player: RegisteredPlayer): F[Unit]

  def joinToRoom(roomUUID: UUID, player: RegisteredPlayer): F[Unit]

  def leaveRoom(roomUUID: UUID, player: RegisteredPlayer): F[Unit]

  def runGame(roomUUID: UUID, player: RegisteredPlayer): F[Unit]

}

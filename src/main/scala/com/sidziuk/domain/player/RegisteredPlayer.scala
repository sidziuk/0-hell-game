package com.sidziuk.domain.player

import com.sidziuk.domain.Player

import java.util.UUID

case class RegisteredPlayer(uuid: UUID, name:String, password: String) extends Player

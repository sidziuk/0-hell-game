package com.sidziuk.player

import java.util.UUID

case class PlayerImpl(uuid: UUID, name:String, password: String) extends Player

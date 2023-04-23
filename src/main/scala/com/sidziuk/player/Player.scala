package com.sidziuk.player

import java.util.UUID

trait Player {
  val uuid: UUID
  val name: String
}

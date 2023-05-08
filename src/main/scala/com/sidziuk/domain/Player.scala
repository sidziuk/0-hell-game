package com.sidziuk.domain

import java.util.UUID

trait Player {
  val uuid: UUID
  val name: String
}

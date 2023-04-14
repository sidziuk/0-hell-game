package com.sidziuk

case class Move(playerId: String, moveType: MoveType, card: Option[Card], bid: Option[Int])

sealed trait MoveType
case object Bid extends MoveType
case object PlayCard extends MoveType

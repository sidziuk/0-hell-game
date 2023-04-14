package com.sidziuk

case class Desk(cards: Option[Map[String, Card]] = None, firstPlayer: Option[(String, Suit)] = None)

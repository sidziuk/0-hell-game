package com.sidziuk.repository

import cats.effect.{Async, Resource}
import doobie.Transactor
import doobie.h2.H2Transactor

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object DbTransactor {

  def geth2Transactor[F[_] : Async]: Resource[F, Transactor[F]] = {

    val customExecutorService = Executors.newFixedThreadPool(4)
    val myExecutionContext: ExecutionContext = ExecutionContext.fromExecutorService(customExecutorService)

    H2Transactor
      .newH2Transactor[F]("jdbc:h2:tcp://localhost/mem:test;DB_CLOSE_DELAY=-1", "sa", "", myExecutionContext)
  }

}

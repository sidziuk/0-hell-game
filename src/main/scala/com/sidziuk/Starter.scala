package com.sidziuk

import cats.effect.kernel.Ref
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.IpLiteralSyntax
import com.sidziuk.domain.room.GameRoom
import com.sidziuk.repository.CreatePlayerTable
import com.sidziuk.repository.DbTransactor.geth2Transactor
import com.sidziuk.repository.game.DoobieGameRepositoryImp
import com.sidziuk.repository.player.DoobiePlayerRepositoryImp
import com.sidziuk.routes.CommonRoutes
import com.sidziuk.servis.game.GameServiceImp
import com.sidziuk.servis.player.PlayerServiceImp
import fs2.concurrent._
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

object Server extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    geth2Transactor[IO].use { transactor =>
      for {
        logger            <- Slf4jLogger.create[IO]
        _                 <- CreatePlayerTable[IO](transactor)
        gameRooms         <- Ref.of[IO, Map[UUID, GameRoom[IO]]](Map.empty)
        roomsTopic        <- Topic[IO, String]

        h2PlayerRepository = DoobiePlayerRepositoryImp[IO](transactor)
        playerService      = new PlayerServiceImp[IO](h2PlayerRepository)

        h2GameRepository = DoobieGameRepositoryImp[IO](transactor)
        gameService      = new GameServiceImp[IO](h2GameRepository)

        exitCode <- {
          val server = EmberServerBuilder
            .default[IO]
            .withHost(ipv4"127.0.0.1")
            .withPort(port"9001")
            .withHttpWebSocketApp(ws =>
              CommonRoutes.getAllRoutes(
                playersService = playerService,
                gameService = gameService,
                gameRooms = gameRooms,
                roomsTopic = roomsTopic,
                logger = logger,
                ws = ws
              )
            )
            .build
            .useForever

          server.as(ExitCode.Success)
        }
      } yield exitCode
    }
  }
}

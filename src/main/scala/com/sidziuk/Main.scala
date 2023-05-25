package com.sidziuk

import cats.effect.kernel.Ref
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.IpLiteralSyntax
import com.sidziuk.domain.room.GameRoom
import com.sidziuk.repository.CreatePlayerTable
import com.sidziuk.repository.DbTransactor.geth2Transactor
import com.sidziuk.repository.player.DoobiePlayerRepository
import com.sidziuk.routes.CommonRoutes
import com.sidziuk.service.game.GameServiceImp
import com.sidziuk.service.player.PlayerServiceImp
import com.sidziuk.service.room.RoomServiceImp
import fs2.concurrent._
import org.http4s.ember.server.EmberServerBuilder

import java.util.UUID

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    geth2Transactor[IO].use { transactor =>
      for {
        _                 <- CreatePlayerTable[IO](transactor)
        gameRooms         <- Ref.of[IO, Map[UUID, GameRoom[IO]]](Map.empty)
        roomsTopic        <- Topic[IO, String]

        h2PlayerRepository = DoobiePlayerRepository[IO](transactor)
        playerService      = new PlayerServiceImp[IO](h2PlayerRepository)

        gameService = new GameServiceImp[IO](playerService, gameRooms)

        gameRoomService = new RoomServiceImp[IO](playerService, roomsTopic, gameRooms)

        exitCode <- {
          val server = EmberServerBuilder
            .default[IO]
            .withHost(ipv4"127.0.0.1")
            .withPort(port"9001")
            .withHttpWebSocketApp(ws =>
              CommonRoutes.getAllRoutes(
                playersService = playerService,
                gameService =gameService,
                roomService = gameRoomService,
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

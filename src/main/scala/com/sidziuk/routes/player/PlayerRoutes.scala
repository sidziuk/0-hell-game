package com.sidziuk.routes.player

import cats.effect.kernel.Ref
import cats.effect.Concurrent
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import com.sidziuk.servis.player.PlayerServiceImp
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl

import java.util.UUID
import cats.Monad
import cats.implicits._
import com.sidziuk.domain.player.RegisteredPlayer
import com.sidziuk.dto.player.CreatePlayerDTO
import io.circe.{Decoder, Encoder}
import io.circe.syntax.EncoderOps

class PlayerRoutes[F[_]: Concurrent: Monad](playersService: PlayerServiceImp[F]) {

  def getPlayerRoutes: HttpRoutes[F] = {

    val dsl = Http4sDsl[F]
    import dsl._

    implicit val directorDecoder: EntityDecoder[F, CreatePlayerDTO]    = jsonOf[F, CreatePlayerDTO]
    implicit val myCaseClassEncoder: EntityEncoder[F, CreatePlayerDTO] = jsonEncoderOf[F, CreatePlayerDTO]

    HttpRoutes.of[F] {

      case req @ POST -> Root / "player" =>
        for {
          createPlayer   <- req.as[CreatePlayerDTO]
          playerIDEither <- playersService.create(createPlayer)
          response       <- playerIDEither match {
                              case Right(playerUUID) => Ok(s"playerUUID: $playerUUID".asJson.noSpaces)
                              case Left(error)       => BadRequest(error)
                            }
        } yield response

      case GET -> Root / "player" / name / password =>
        for {
          playerIDOption       <- playersService.get(name, password)
          response <- playerIDOption match {
            case Some(playerUUID) => Ok(s"playerUUID: $playerUUID".asJson.noSpaces)
            case None => NotFound(s"Player with name $name not found}")
          }
        } yield response

      case DELETE -> Root / "player" / id =>
        for {
          playerIDOption <- playersService.delete(id)
          response <- playerIDOption match {
            case Some(playerUUID) => Ok(s"playerUUID: $playerUUID".asJson.noSpaces)
            case None => NotFound(s"Player with id $id not found}")
          }
        } yield response
    }
  }
}

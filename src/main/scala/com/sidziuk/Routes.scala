//import sttp.tapir._
//import sttp.tapir.json.circe._
//
//object Routes {
//  def getRoutes(
//                 players: Ref[IO, Map[UUID, PlayerImpl]],
//                 gameRooms: Ref[IO, Map[UUID, GameRoom]],
//                 roomsTopic: Topic[IO, String],
//                 logger: SelfAwareStructuredLogger[IO],
//                 ws: WebSocketBuilder2[IO]
//               ): HttpRoutes[IO] = {
//    HttpRoutes.of[IO] {
//
//      // POST /player
//      Endpoint
//        .post
//        .in("player")
//        .in(jsonBody[CreatePlayerDTO])
//        .out(jsonBody[PlayerImpl])
//        .description("Create a new player")
//        .name("createPlayer")
//        .tag("player")
//        .toRoute { createPlayerDTO =>
//          // implementation
//        }
//
//      // GET /players/{name}/{password}
//      Endpoint
//        .get
//        .in("players" / path[String]("name") / path[String]("password"))
//        .out(jsonBody[PlayerImpl])
//        .description("Get player by name and password")
//        .name("getPlayerByNameAndPassword")
//        .tag("player")
//        .toRoute { (name, password) =>
//          // implementation
//        }
//
//      // GET /ws/room/{playerUUID}
//      Endpoint
//        .get
//        .in("ws" / "room" / path[UUID]("playerUUID"))
//        .out(webSocketBody[WebSocketDTO, WebSocketDTO](ws.build()))
//        .description("Join a room as a spectator")
//        .name("joinRoomAsSpectator")
//        .tag("room")
//        .toRoute { playerUUID =>
//          // implementation
//        }
//
//      // GET /ws/game/{playerUUID}/{roomUUID}
//      Endpoint
//        .get
//        .in("ws" / "game" / path[UUID]("playerUUID") / path[UUID]("roomUUID"))
//        .out(webSocketBody[WebSocketDTO, WebSocketDTO](ws.build()))
//        .description("Join a room as a player")
//        .name("joinRoomAsPlayer")
//        .tag("room")
//        .toRoute { (playerUUID, roomUUID) =>
//          // implementation
//        }
//    }
//  }
//}

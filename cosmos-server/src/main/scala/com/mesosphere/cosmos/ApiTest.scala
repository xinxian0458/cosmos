package com.mesosphere.cosmos

import com.netaporter.uri.Uri
import io.circe.Json
import io.circe.JsonObject

import com.mesosphere.cosmos.http.MediaTypes
import com.mesosphere.cosmos.model.PackageRepository
import com.mesosphere.cosmos.model.PackageRepositoryListRequest
import com.mesosphere.cosmos.model.PackageRepositoryListResponse
import com.mesosphere.cosmos.raml.Schemafier
import com.mesosphere.cosmos.raml.ObjectSchemafier

object ApiTest extends App {
  case class World(w: Int, o: String, r: Int)
  case class HelloWorld(hello: String, world: World)

  implicit val worldSchemafier = ObjectSchemafier.deriveFor[World].decorateProperties {
    case ("w", schemafier) =>
      schemafier
        .withTitle("w field title")
    case ("o", schemafier) =>
      schemafier
        .withDescription("o field description")
  }

  val objectSchemafier = ObjectSchemafier.deriveFor[HelloWorld].decorateProperties {
    case ("hello", schemafier) =>
      schemafier
        .withTitle("hello field title")
        .withDescription("hello field description")

    case ("world", schemafier) =>
      schemafier
        .withTitle("world field title")
        .withDescription("world field description")
  } withTitle {
    "helloworld object title"
  } withDescription {
    "helloworld object description"
  }

  implicit val uriSchemafier = Schemafier.schemafyString.as[Uri]

  implicit val packageRepositorySchemafier = {
    ObjectSchemafier.deriveFor[PackageRepository].decorateProperties {
      case ("name", schemafier) =>
        schemafier.withDescription("Unique name for the repository")
      case ("uri", schemafier) =>
        schemafier.withDescription(
          "HTTP URL for the repository. The URL must resolve to a Zip file defined by " +
          "https://github.com/mesosphere/universe/blob/version-2.x/README.md"
        )
    }
  }

  implicit val packageRepositoryListResponseSchemafier = {
    ObjectSchemafier.deriveFor[PackageRepositoryListResponse].decorateProperties {
      case ("repositories", schemafier) =>
        schemafier.withDescription("List of repositories")
    }
  }

  implicit val packageRepositoryListRequestSchemafier = {
    ObjectSchemafier.deriveFor[PackageRepositoryListRequest]
  }

  val resource = Json.fromFields(
    List(
      ("title", Json.string("Cosmos API")),
      (
        "/package/repo/list",
        Json.fromFields(
          List(
            (
              "post",
              Json.fromFields(
                List(
                  ("description", Json.string("List configured Universe repositories")),
                  (
                    "body",
                    Json.fromFields(
                      List(
                        (
                          MediaTypes.PackageRepositoryListRequest.show, Json.fromFields(
                            List(
                              (
                                "schema",
                                Json.string(
                                  Json.fromJsonObject(
                                    packageRepositoryListRequestSchemafier.schema
                                  ).toString
                                )
                              )
                            )
                          )
                        )
                      )
                    )
                  ),
                  ("responses",
                    Json.fromFields(
                      List(
                        (
                          "200",
                          Json.fromFields(
                            List(
                              (
                                "body",
                                Json.fromFields(
                                  List(
                                    (
                                      MediaTypes.PackageRepositoryListResponse.show,
                                      Json.fromFields(
                                        List(
                                          (
                                            "schema",
                                            Json.string(
                                              Json.fromJsonObject(
                                                packageRepositoryListResponseSchemafier.schema
                                              ).toString
                                            )
                                          )
                                        )
                                      )
                                    )
                                  )
                                )
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  println(resource)
}

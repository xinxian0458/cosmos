package com.mesosphere.cosmos

import io.circe.Json
import io.circe.JsonObject
import com.twitter.finagle.http.Status

import com.mesosphere.cosmos.http.MediaTypes
import com.mesosphere.cosmos.model.PackageRepository
import com.mesosphere.cosmos.model.PackageRepositoryListRequest
import com.mesosphere.cosmos.model.PackageRepositoryListResponse
import com.mesosphere.cosmos.raml.ObjectSchemafier
import com.mesosphere.cosmos.raml.Schemafiers.PackageRepositoryListRequestSchemafier
import com.mesosphere.cosmos.raml.Schemafiers.PackageRepositoryListResponseSchemafier
import com.mesosphere.cosmos.raml.Schemafier
import com.mesosphere.cosmos.raml.Document
import com.mesosphere.cosmos.raml.Resource
import com.mesosphere.cosmos.raml.Method
import com.mesosphere.cosmos.raml.Body
import com.mesosphere.cosmos.raml.DataType
import com.mesosphere.cosmos.raml.Response

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

  val resource = Document(
    "Cosmos API",
    Map(
      (
        "/package/repo/list",
        Resource(
          post=Some(
            Method(
              description=Some("List configured Universe repositories"),
              body=Some(
                Body(
                  Map(
                    (
                      MediaTypes.PackageRepositoryListRequest,
                      DataType(
                        Some(
                          Json.fromJsonObject(
                            PackageRepositoryListRequestSchemafier.schema
                          ).toString
                        )
                      )
                    )
                  )
                )
              ),
              responses=Map(
                (
                  Status.Ok,
                  Response(
                    body=Some(
                      Body(
                        Map(
                          (
                            MediaTypes.PackageRepositoryListRequest,
                            DataType(
                              Some(
                                Json.fromJsonObject(
                                  PackageRepositoryListResponseSchemafier.schema
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

  println(resource)
}

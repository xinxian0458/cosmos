package com.mesosphere.cosmos

import scala.collection.generic.IsTraversableOnce
import scala.language.higherKinds
import scala.reflect.ClassTag

import cats.std.list.listInstance
import com.netaporter.uri.Uri
import io.circe.Json
import io.circe.JsonObject
import shapeless.::
import shapeless.HList
import shapeless.HNil
import shapeless.LabelledGeneric
import shapeless.LabelledGeneric.Aux
import shapeless.Lazy
import shapeless.Witness
import shapeless.labelled.FieldType
import shapeless.ops.record.Keys

import com.mesosphere.cosmos.model.PackageRepository
import com.mesosphere.cosmos.model.PackageRepositoryListRequest
import com.mesosphere.cosmos.model.PackageRepositoryListResponse
import com.mesosphere.cosmos.http.MediaTypes

trait Schemafier[T] { parent =>
  def schema: JsonObject

  final def as[U]: Schemafier[U] = new Schemafier[U] {
    override final def schema: JsonObject = parent.schema
  }

  final def withTitle(title: String): Schemafier[T] = new Schemafier[T] {
    override final def schema: JsonObject = {
      parent.schema + ("title", Json.string(title))
    }
  }

  final def withDescription(description: String): Schemafier[T] = new Schemafier[T] {
    override final def schema: JsonObject = {
      parent.schema + ("description", Json.string(description))
    }
  }
}

object Schemafier {
  implicit final def schemafyString[T <: String]: Schemafier[T] = new Schemafier[T] {
    override final def schema: JsonObject = {
      JsonObject.empty + ("type", Json.string("string"))
    }
  }

  implicit final def schemafyInt[T <: Int]: Schemafier[T] = new Schemafier[T] {
    override final def schema: JsonObject = {
      JsonObject.empty + ("type", Json.string("integer"))
    }
  }

  implicit final def schemafyTraversableOnce[T, R[_]](
    implicit
    schemafier: Schemafier[T],
    isTraversable: IsTraversableOnce[R[T]] { type A = T }
  ): Schemafier[R[T]] = new Schemafier[R[T]] {
    override final def schema: JsonObject = {
      JsonObject.empty +
      ("type", Json.string("array")) +
      ("items", Json.fromJsonObject(schemafier.schema))
    }
  }
}

trait ObjectSchemafier[T] extends Schemafier[T] { parent =>
  val properties: List[(String, Schemafier[_])]

  override final def schema: JsonObject = {
    val jsonProperties = properties.map { case (field, schemafier) =>
      (field, Json.fromJsonObject(schemafier.schema))
    }

    JsonObject.empty +
    ("type", Json.string("object")) +
    ("properties", Json.fromFields(jsonProperties))
  }

  final def decorateProperties(
    decorator: PartialFunction[(String, Schemafier[_]), Schemafier[_]]
  ): ObjectSchemafier[T] = {
    new ObjectSchemafier[T] {
      val properties = parent.properties.map { case (field, schemafier) =>
        (field, decorator.orElse(default)((field, schemafier)))
      }
    }
  }

  private final val default: PartialFunction[(String, Schemafier[_]), Schemafier[_]] = {
    case (_, schemafier) => schemafier
  }
}

object ObjectSchemafier {
  implicit final val schemafyHNil: ObjectSchemafier[HNil] = {
    new ObjectSchemafier[HNil] {
      override final val properties: List[(String, Schemafier[_])] = List.empty
    }
  }

  implicit final def schemafyLabelledHList[K <: Symbol, V, T <: HList](
    implicit
    key: Witness.Aux[K],
    headSchemafier: Schemafier[V],
    tailSchemafier: ObjectSchemafier[T]
  ): ObjectSchemafier[FieldType[K, V] :: T] = {
    new ObjectSchemafier[FieldType[K, V] :: T] {
      override final val properties: List[(String, Schemafier[_])] = {
        (key.value.name, headSchemafier) :: tailSchemafier.properties
      }
    }
  }

  implicit final def schemafyCaseClass[T, R <: HList](
    implicit
    gen: Aux[T, R],
    hListSchemafier: ObjectSchemafier[R]
  ): ObjectSchemafier[T] = {
    new ObjectSchemafier[T] {
      val properties = hListSchemafier.properties
    }
  }

  final def deriveFor[T](implicit objectSchemafier: ObjectSchemafier[T]): ObjectSchemafier[T] = {
    objectSchemafier
  }
}

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

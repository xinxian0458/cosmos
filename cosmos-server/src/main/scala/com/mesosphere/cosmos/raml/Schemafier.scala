package com.mesosphere.cosmos.raml

import scala.collection.generic.IsTraversableOnce
import scala.language.higherKinds

import io.circe.Json
import io.circe.JsonObject
import cats.implicits.listInstance


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
      JsonObject.singleton("type", Json.string("string"))
    }
  }

  implicit final def schemafyInt[T <: Int]: Schemafier[T] = new Schemafier[T] {
    override final def schema: JsonObject = {
      JsonObject.singleton("type", Json.string("integer"))
    }
  }

  implicit final def schemafyTraversableOnce[T, R[_]](
    implicit
    schemafier: Schemafier[T],
    isTraversable: IsTraversableOnce[R[T]] { type A = T }
  ): Schemafier[R[T]] = new Schemafier[R[T]] {
    override final def schema: JsonObject = {
      JsonObject.from(
        List(
          ("type", Json.string("array")),
          ("items", Json.fromJsonObject(schemafier.schema))
        )
      )
    }
  }
}

package com.mesosphere.cosmos.raml

import io.circe.Json
import io.circe.JsonObject
import shapeless.::
import shapeless.HList
import shapeless.HNil
import shapeless.LabelledGeneric
import shapeless.Witness
import shapeless.labelled.FieldType

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
    gen: LabelledGeneric.Aux[T, R],
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

package com.mesosphere.cosmos.raml

import com.twitter.finagle.http.Status

case class Method(
  description: Option[String],
  body: Option[Body],
  responses: Map[Status, Response]
)

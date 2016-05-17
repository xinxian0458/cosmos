package com.mesosphere.cosmos.raml

import com.mesosphere.cosmos.http.MediaType

case class Body(
  content: Map[MediaType, DataType]
)

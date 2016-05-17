package com.mesosphere.cosmos.raml

case class Resource(
  post: Option[Method] = None,
  get: Option[Method] = None,
  put: Option[Method] = None,
  delete: Option[Method] = None
)

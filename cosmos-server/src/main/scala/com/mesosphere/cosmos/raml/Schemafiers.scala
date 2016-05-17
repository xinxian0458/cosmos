package com.mesosphere.cosmos.raml

import com.netaporter.uri.Uri

import com.mesosphere.cosmos.model.PackageRepositoryListRequest
import com.mesosphere.cosmos.model.PackageRepositoryListResponse
import com.mesosphere.cosmos.model.PackageRepository

object Schemafiers {
  implicit final val UriSchemafier = Schemafier.schemafyString.as[Uri] 

  implicit final val PackageRepositorySchemafier = {
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

  implicit final val PackageRepositoryListResponseSchemafier = {
    ObjectSchemafier.deriveFor[PackageRepositoryListResponse].decorateProperties {
      case ("repositories", schemafier) =>
        schemafier.withDescription("List of repositories")
    }
  }

  implicit final val PackageRepositoryListRequestSchemafier = {
    ObjectSchemafier.deriveFor[PackageRepositoryListRequest]
  }
}

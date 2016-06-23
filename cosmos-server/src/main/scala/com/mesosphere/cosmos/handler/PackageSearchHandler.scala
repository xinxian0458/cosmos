package com.mesosphere.cosmos.handler

import com.mesosphere.cosmos.http.RequestSession
import com.mesosphere.cosmos.MultiRepository
import com.mesosphere.cosmos.rpc
import com.twitter.util.Future

private[cosmos] final class PackageSearchHandler(
    packageCache: MultiRepository
)
    extends EndpointHandler[
        rpc.v1.model.SearchRequest, rpc.v1.model.SearchResponse] {

  override def apply(
      request: rpc.v1.model.SearchRequest)(implicit session: RequestSession)
    : Future[rpc.v1.model.SearchResponse] = {
    packageCache.packages.map { repositories =>
      val sortedSearches =
        MultiRepository.search(repositories, request.query).sortBy { search =>
          (!search.selected.getOrElse(false), search.name.toLowerCase)
        }
      rpc.v1.model.SearchResponse(sortedSearches)
    }
  }
}

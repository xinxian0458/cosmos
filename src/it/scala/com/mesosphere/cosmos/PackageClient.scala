package com.mesosphere.cosmos

import cats.data.Xor.Right
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.RequestBuilder
import com.twitter.finagle.http.Response
import com.twitter.io.Buf
import com.twitter.util.Future
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json

import com.mesosphere.cosmos.http.MediaTypes
import com.mesosphere.cosmos.model.ListRequest
import com.mesosphere.cosmos.model.ListResponse

final class PackageClient(service: Service[Request, Response], baseUri: Uri) {

  // TODO: This should return an Future[Xor[ListResponse, CosmosError]]
  def list(
    request: ListRequest
  )(implicit
    encoder: Encoder[ListRequest], decoder: Decoder[ListResponse]
  ): Future[ListResponse] = {
    service(
      RequestBuilder()
      .url((baseUri / "package" / "list").toString)
      .setHeader("Accept", MediaTypes.ListResponse.show)
      .setHeader("Content-Type", MediaTypes.ListRequest.show)
      .buildPost(Buf.Utf8(encoder(request).noSpaces))
    ).map { response =>
      println("Response body.....")
      println(response.status)
      println(response.getContentString())
      val Right(success) = decoder.decodeJson(Json.string(response.getContentString()))
      success
    }
  }
}

object PackageClient {
  def apply(service: Service[Request, Response], baseUri: Uri): PackageClient = {
    new PackageClient(service, baseUri)
  }
}

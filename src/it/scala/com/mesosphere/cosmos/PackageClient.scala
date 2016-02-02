package com.mesosphere.cosmos

import cats.data.Xor.Right
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl.uriToUriOps
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

final class PackageClient(service: Service[Request, Response], baseUri: Uri) {

  // TODO: This should return an Future[Xor[InstallResponse, CosmosError]]
  def install(
    request: model.InstallRequest
  )(implicit
    encoder: Encoder[model.InstallRequest], decoder: Decoder[model.InstallResponse]
  ): Future[model.InstallResponse] = {
    service(
      RequestBuilder()
      .url((baseUri / "v1" / "package" / "install").toString)
      .setHeader("Accept", MediaTypes.InstallResponse.show)
      .setHeader("Content-Type", MediaTypes.InstallRequest.show)
      .buildPost(Buf.Utf8(encoder(request).noSpaces))
    ).map { response =>
      println("Install.....")
      println(response.getContentString())
      val Right(success) = decoder.decodeJson(Json.string(response.getContentString()))
      success
    }
  }

  // TODO: This should return an Future[Xor[UninstallResponse, CosmosError]]
  def uninstall(
    request: model.UninstallRequest
  )(implicit
  encoder: Encoder[model.UninstallRequest], decoder: Decoder[model.UninstallResponse]
  ): Future[model.UninstallResponse] = {
    service(
      RequestBuilder()
      .url((baseUri / "v1" / "package" / "uninstall").toString)
      .setHeader("Accept", MediaTypes.UninstallResponse.show)
      .setHeader("Content-Type", MediaTypes.UninstallRequest.show)
      .buildPost(Buf.Utf8(encoder(request).noSpaces))
    ).map { response =>
      println("Uninstall.....")
      println(response.getContentString())
      val Right(success) = decoder.decodeJson(Json.string(response.getContentString()))
      success
    }
  }

  // TODO: This should return an Future[Xor[ListResponse, CosmosError]]
  def list(
    request: model.ListRequest
  )(implicit
    encoder: Encoder[model.ListRequest], decoder: Decoder[model.ListResponse]
  ): Future[model.ListResponse] = {
    service(
      RequestBuilder()
      .url((baseUri / "v1" / "package" / "list").toString)
      .setHeader("Accept", MediaTypes.ListResponse.show)
      .setHeader("Content-Type", MediaTypes.ListRequest.show)
      .buildPost(Buf.Utf8(encoder(request).noSpaces))
    ).map { response =>
      println("List.....")
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

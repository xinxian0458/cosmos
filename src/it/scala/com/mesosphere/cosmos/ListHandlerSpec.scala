package com.mesosphere.cosmos

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import com.netaporter.uri.dsl.stringToUri
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.util.Await
import org.apache.commons.io.FileUtils
import org.scalatest.Matchers
import org.scalatest.Outcome
import org.scalatest.fixture

import com.mesosphere.cosmos.circe.Decoders.decodeInstallResponse
import com.mesosphere.cosmos.circe.Decoders.decodeListResponse
import com.mesosphere.cosmos.circe.Decoders.decodeUninstallResponse
import com.mesosphere.cosmos.circe.Encoders.encodeInstallRequest
import com.mesosphere.cosmos.circe.Encoders.encodeListRequest
import com.mesosphere.cosmos.circe.Encoders.encodeUninstallRequest

final class ListHandlerSpec extends fixture.FlatSpec with Matchers {

  case class FixtureParam(service: Service[Request, Response])

  private[this] val baseUri = "http://localhost"

  override def withFixture(test: OneArgTest): Outcome = {
    // Create temporary folder
    val tempPath = Files.createTempDirectory("cosmos-ListHandlerSpec")

    val service = Cosmos.service(dcosHost(), universeBundleUri(), tempPath)

    try {
      super.withFixture(test.toNoArgTest(FixtureParam(service)))
    } finally {
      // Delete the temporary folder
      val _ = FileUtils.deleteQuietly(tempPath.toFile)
    }
  }

  it should "list all services" in { fixture =>
    val client = PackageClient(fixture.service, baseUri)
    Await.result(client.install(model.InstallRequest("marathon")))
    Await.result(client.install(model.InstallRequest("chronos")))
    val response = Await.result(client.list(model.ListRequest()))
    println(response)

    // TODO: Uninstall a bunch of services
    Await.result(client.uninstall(model.UninstallRequest("marathon", None, None)))
    Await.result(client.uninstall(model.UninstallRequest("chronos", None, None)))

    response shouldBe model.ListResponse(None)
  }

  it should "list by package name" in { service =>
  }

  it should "list by app id" in { service =>
  }

  it should "list by both package name and app id" in { service =>
  }

  it should "list missing package name" in { service =>
  }

  it should "list missing app id" in { service =>
  }

  it should "list missin package name and app id" in { service =>
  }
}

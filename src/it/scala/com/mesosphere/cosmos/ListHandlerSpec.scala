package com.mesosphere.cosmos

import java.nio.file.Files

import com.netaporter.uri.dsl.stringToUri
import com.twitter.finagle.Service
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.http.Status
import com.twitter.util.Await
import org.scalatest.Matchers
import org.scalatest.Outcome
import org.scalatest.fixture

import com.mesosphere.cosmos.circe.Decoders.decodeListResponse
import com.mesosphere.cosmos.circe.Encoders.encodeListRequest
import com.mesosphere.cosmos.model.ListRequest
import com.mesosphere.cosmos.model.ListResponse

final class ListHandlerSpec extends fixture.FlatSpec with Matchers {

  case class FixtureParam(service: Service[Request, Response])

  override def withFixture(test: OneArgTest): Outcome = {
    // Create temporary folder
    val tempDir = Files.createTempDirectory("cosmos-ListHandlerSpec").toFile
    val absolutePath = tempDir.getAbsolutePath
    //logger.info("Setting com.mesosphere.cosmos.universeCacheDir={}", absolutePath)
    System.setProperty("com.mesosphere.cosmos.universeCacheDir", absolutePath)

    // Install a bunch of services


    try {
      super.withFixture(test.toNoArgTest(FixtureParam(Cosmos.service)))
    } finally {
      // Uninstall a bunch of services
      // Delete the temporary folder
      val _ = tempDir.delete()
    }
  }

  val baseUri = "http://localhost"

  it should "list all services" in { fixture =>
    val client = PackageClient(fixture.service, baseUri)
    val response = Await.result(client.list(ListRequest()))
    response shouldBe ListResponse(Nil)
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

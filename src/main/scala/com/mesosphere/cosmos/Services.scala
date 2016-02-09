package com.mesosphere.cosmos

import java.net.InetSocketAddress

import com.netaporter.uri.Uri
import com.twitter.finagle.client.Transporter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.ssl.Ssl
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.{ChannelWriteException, Http, Service, SimpleFilter}
import com.twitter.util.{Future, Try}


object Services {
  def adminRouterClient(uri: Uri): Try[Service[Request, Response]] = {
    httpClient("adminRouter", uri)
  }

  def marathonClient(uri: Uri): Try[Service[Request, Response]] = {
    httpClient("marathon", uri)
  }

  def mesosClient(uri: Uri): Try[Service[Request, Response]] = {
    httpClient("mesos", uri)
  }

  def httpClient(serviceName: String, uri: Uri): Try[Service[Request, Response]] = {
    extractHostAndPort(uri) map { case ConnectionDetails(hostname, port, tls) =>
      val cBuilder = tls match {
        case false =>
          Http.client
        case true =>
          Http.client
            .configured(Transport.TLSClientEngine(Some({
              case inet: InetSocketAddress => Ssl.client(hostname, inet.getPort)
              case _ => Ssl.client()
            })))
            .configured(Transporter.TLSHostname(Some(hostname)))
      }

      new ConnectionExceptionHandler(serviceName) andThen cBuilder.newService(s"$hostname:$port", serviceName)
    }
  }

  private[cosmos] def extractHostAndPort(uri: Uri): Try[ConnectionDetails] = Try {
    (uri.scheme, uri.host, uri.port) match {
      case (Some("https"), Some(h), p) => ConnectionDetails(h, p.getOrElse(443), tls = true)
      case (Some("http"), Some(h), p) => ConnectionDetails(h, p.getOrElse(80), tls = false)
      case (_, _, _) => throw err(uri.toString)
    }
  }

  private def err(actual: String): Throwable = {
    new IllegalArgumentException(s"Unsupported or invalid URI. Expected format 'http[s]://example.com[:port][/base-path]' actual '$actual'")
  }

  private[cosmos] case class ConnectionDetails(host: String, port: Int, tls: Boolean = false)

  private[this] class ConnectionExceptionHandler(serviceName: String) extends SimpleFilter[Request, Response] {
    override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
      service(request)
        .rescue {
          case ce: ChannelWriteException =>
            Future.exception(new ServiceUnavailable(serviceName, ce))
        }
    }
  }
}

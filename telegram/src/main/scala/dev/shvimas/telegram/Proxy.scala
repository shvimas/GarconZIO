package dev.shvimas.telegram

import com.softwaremill.sttp.SttpBackendOptions

sealed trait Proxy {
  val host: String
  val port: Int
  val auth: Option[ProxyAuth]

  def toSttpBackendProxy: SttpBackendOptions.Proxy = {
    val proxyType =
      this match {
        case _: SocksProxy => SttpBackendOptions.ProxyType.Socks
        case _: HttpProxy  => SttpBackendOptions.ProxyType.Http
      }

    SttpBackendOptions.Proxy(
        host = host,
        port = port,
        proxyType = proxyType,
        nonProxyHosts = Nil,
        auth = auth.map(_.toSttpBackendProxyAuth),
    )
  }
}

final case class SocksProxy(host: String, port: Int, auth: Option[ProxyAuth]) extends Proxy

final case class HttpProxy(host: String, port: Int, auth: Option[ProxyAuth]) extends Proxy

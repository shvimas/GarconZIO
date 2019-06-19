package dev.shvimas.telegram

import com.softwaremill.sttp.SttpBackendOptions

sealed trait ProxyAuth {

  def toSttpBackendProxyAuth: SttpBackendOptions.ProxyAuth =
    this match {
      case p: ProxyAuthUsernamePassword =>
        SttpBackendOptions.ProxyAuth(
            username = p.username,
            password = p.password,
        )
    }
}

case class ProxyAuthUsernamePassword(username: String, password: String) extends ProxyAuth

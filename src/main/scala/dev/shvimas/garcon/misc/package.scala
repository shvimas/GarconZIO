package dev.shvimas.garcon

import zio.Has

package object misc {
  type SafeRandom = Has[SafeRandom.Service]
}

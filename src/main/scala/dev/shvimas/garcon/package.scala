package dev.shvimas

import dev.shvimas.garcon.database.Database
import zio.Has

package object garcon {
  type HasDatabase = Has[Database.Service]
  type Translators = Has[Translators.Service]
}

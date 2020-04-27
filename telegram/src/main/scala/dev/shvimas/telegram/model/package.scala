package dev.shvimas.telegram

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

package object model {
  type GetMeResult       = Result[User]
  type GetUpdatesResult  = Result[List[Update]]
  type SendMessageResult = Result[Message]
  type BooleanResult     = Result[Boolean]

  object JsonImplicits {
    // codec config\ must be inlined as macro cannot have external dependencies
    implicit val codec4GetMeResult: JsonValueCodec[GetMeResult] = {
      JsonCodecMaker.make(
        CodecMakerConfig
          .withFieldNameMapper(JsonCodecMaker.enforce_snake_case)
          .withAllowRecursiveTypes(true)
      )
    }
    implicit val codec4GetUpdatesResult: JsonValueCodec[GetUpdatesResult] = {
      JsonCodecMaker.make(
        CodecMakerConfig
          .withFieldNameMapper(JsonCodecMaker.enforce_snake_case)
          .withAllowRecursiveTypes(true)
      )
    }
    implicit val codec4SendMessageResult: JsonValueCodec[SendMessageResult] = {
      JsonCodecMaker.make(
        CodecMakerConfig
          .withFieldNameMapper(JsonCodecMaker.enforce_snake_case)
          .withAllowRecursiveTypes(true)
      )
    }
    implicit val codec4BooleanResult: JsonValueCodec[BooleanResult] = {
      JsonCodecMaker.make(
        CodecMakerConfig
          .withFieldNameMapper(JsonCodecMaker.enforce_snake_case)
          .withAllowRecursiveTypes(true)
      )
    }
  }
}

package dev.shvimas.telegram.model

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import dev.shvimas.telegram.model.InlineKeyboardMarkup.codec

case class InlineKeyboardMarkup(inlineKeyboard: Seq[Seq[InlineKeyboardButton]]) {
  def toJson: String = writeToArray(this).map(_.toChar).mkString
}

object InlineKeyboardMarkup {

  def makeRow(row: InlineKeyboardButton*): InlineKeyboardMarkup =
    new InlineKeyboardMarkup(Seq(row))

  implicit val codec: JsonValueCodec[InlineKeyboardMarkup] = {
    JsonCodecMaker.make[InlineKeyboardMarkup](
        CodecMakerConfig
          .withFieldNameMapper(JsonCodecMaker.enforce_snake_case)
    )
  }
}

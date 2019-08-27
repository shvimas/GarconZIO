package dev.shvimas.telegram.model

import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.JsonMethods.{pretty, render}
import org.json4s.Extraction.decompose

case class InlineKeyboardMarkup(inlineKeyboard: Seq[Seq[InlineKeyboardButton]]) {
  import InlineKeyboardMarkup.formats

  def toJson: String =
    pretty(render(decompose(this).snakizeKeys))
}

object InlineKeyboardMarkup {
  implicit private val formats: Formats = DefaultFormats

  def makeRow(row: InlineKeyboardButton*): InlineKeyboardMarkup =
    new InlineKeyboardMarkup(Seq(row))
}

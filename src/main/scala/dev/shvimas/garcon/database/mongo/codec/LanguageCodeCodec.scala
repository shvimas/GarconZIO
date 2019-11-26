package dev.shvimas.garcon.database.mongo.codec

import dev.shvimas.translate.LanguageCode
import dev.shvimas.translate.LanguageCode.LanguageCode
import org.bson.{BsonReader, BsonWriter}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}

object LanguageCodeCodec extends Codec[LanguageCode] {
  override def decode(reader: BsonReader, decoderContext: DecoderContext): LanguageCode =
    LanguageCode.withName(reader.readString())

  override def encode(writer: BsonWriter, value: LanguageCode, encoderContext: EncoderContext): Unit =
    writer.writeString(value.toString)

  override def getEncoderClass: Class[LanguageCode] = classOf[LanguageCode]
}

object LanguageCodeCodecProvider extends CodecProvider {
  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] =
    if (classOf[LanguageCode.Value].isAssignableFrom(clazz))
      LanguageCodeCodec.asInstanceOf[Codec[T]]
    else null
}

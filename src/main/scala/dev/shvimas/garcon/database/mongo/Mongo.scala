package dev.shvimas.garcon.database.mongo

import com.mongodb.ConnectionString
import com.typesafe.scalalogging.StrictLogging
import dev.shvimas.garcon.MainConfig.config
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.database.model._
import dev.shvimas.garcon.database.mongo.codec.LanguageCodeCodecProvider
import dev.shvimas.garcon.database.mongo.model._
import dev.shvimas.translate.LanguageDirection
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.model.{ReplaceOptions, UpdateOptions}
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, max, set}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import zio.{Task, ZIO}

import scala.concurrent.Future

object Mongo {

  trait Instance extends Database with StrictLogging {

    import Config._
    import Helpers._

    protected val garconDb: MongoDatabase = client.getDatabase(DbName.garcon)

    protected val globalsColl: MongoCollection[MongoGlobals] =
      garconDb.getCollection(CollName.globals)

    protected val usersDataColl: MongoCollection[MongoUserData] =
      garconDb.getCollection(CollName.usersData)

    protected def fromFuture[A](future: => Future[A]): Task[A] =
      ZIO.fromFuture(implicit ec => future)

    implicit protected class RichSingleObservable[T](inner: SingleObservable[T]) {
      def toTask: Task[T]               = fromFuture(inner.toFuture())
      def toOptionTask: Task[Option[T]] = fromFuture(inner.toFutureOption())
    }

    implicit protected class RichObservable[T](inner: Observable[T]) {
      def toSeqTask: Task[Seq[T]]       = fromFuture(inner.toFuture())
      def toOptionTask: Task[Option[T]] = fromFuture(inner.headOption())
    }

    def getGlobals: Task[Option[MongoGlobals]] =
      globalsColl
        .find()
        .first()
        .toOptionTask

    override def updateOffset(offset: Long): Task[UpdateResult] =
      globalsColl
        .updateOne(
            filter = emptyBson,
            update = max(GlobalsFields.offset, offset),
            options = upsert
        )
        .toTask

    override def getOffset: Task[Long] =
      getGlobals map {
        case Some(globals) => globals.offset.getOrElse(0)
        case None          => 0
      }

    private def getWordsColl(chatId: Int): MongoCollection[MongoCommonTranslation] =
      garconDb.getCollection(s"${chatId}_words")

    private def getTranslation(chatId: Int,
                               text: String,
                               languageDirection: LanguageDirection,
    ): Task[Option[MongoCommonTranslation]] =
      getWordsColl(chatId)
        .find(
            filter = combine(
                equal(CommonTranslationFields.text, text),
                equal(
                    CommonTranslationFields.languageDirection,
                    MongoLanguageDirection(languageDirection)
                )
            )
        )
        .first()
        .toOptionTask

    override def lookUpText(text: String,
                            languageDirection: LanguageDirection,
                            chatId: Int,
    ): Task[Option[CommonTranslation]] =
      getTranslation(chatId, text, languageDirection)
        .map(_.map(_.toCommonTranslation))

    override def addCommonTranslation(translation: CommonTranslation,
                                      chatId: Int,
                                      languageDirection: LanguageDirection,
                                      messageId: Int,
    ): Task[UpdateResult] = {
      val mongoLanguageDirection = MongoLanguageDirection(languageDirection)
      getWordsColl(chatId)
        .replaceOne(
            filter = combine(
                equal(CommonTranslationFields.text, translation.originalText),
                equal(CommonTranslationFields.languageDirection, mongoLanguageDirection)
            ),
            replacement = MongoCommonTranslation(translation, mongoLanguageDirection, messageId),
            options = repsert
        )
        .toTask
    }

    override def deleteText(text: String, langDirection: LanguageDirection, chatId: Int): Task[DeleteResult] =
      getWordsColl(chatId)
        .deleteOne(
            filter = combine(
                equal(CommonTranslationFields.text, text),
                equal(
                    CommonTranslationFields.languageDirection,
                    MongoLanguageDirection(langDirection)
                )
            )
        )
        .toTask

    override def getUserData(chatId: Int): Task[Option[UserData]] =
      usersDataColl
        .find(filter = equal(UserDataFields.chatId, chatId))
        .first()
        .toOptionTask
        .map(convertUserData)

    override def setUserData(userData: UserData): Task[UpdateResult] =
      usersDataColl
        .replaceOne(
            filter = equal(UserDataFields.chatId, userData.chatId),
            replacement = MongoUserData(userData),
            repsert,
        )
        .toTask

    override def setLanguageDirection(chatId: Int, languageDirection: LanguageDirection): Task[UpdateResult] =
      usersDataColl
        .updateOne(
            equal(UserDataFields.chatId, chatId),
            set(UserDataFields.langDir, MongoLanguageDirection(languageDirection)),
            upsert
        )
        .toTask

    override def findLanguageDirectionForMessage(chatId: Int,
                                                 text: String,
                                                 messageId: Int,
    ): Task[Option[LanguageDirection]] =
      getWordsColl(chatId)
        .find(
            combine(
                equal(CommonTranslationFields.text, text),
                equal(CommonTranslationFields.messageId, messageId)
            )
        )
        .map(_.languageDirection.toLanguageDirection)
        .toOptionTask

    override def editTranslation(text: String,
                                 edit: String,
                                 languageDirection: LanguageDirection,
                                 chatId: Int,
    ): Task[Option[UpdateResult]] =
      getTranslation(chatId, text, languageDirection).flatMap {
        case Some(mongoCommonTranslation) =>
          mongoCommonTranslation.messagedId match {
            case Some(messageId) =>
              val edited = mongoCommonTranslation.copy(edited = Some(edit)).toCommonTranslation
              addCommonTranslation(edited, chatId, languageDirection, messageId).map(Some(_))
            case None => ZIO.none
          }
        case None => ZIO.none
      }

    override def getRandomWord(chatId: Int, languageDirection: LanguageDirection): Task[Option[CommonTranslation]] =
      getWordsColl(chatId)
        .aggregate(
            Seq(
                `match`(
                    equal(
                        CommonTranslationFields.languageDirection,
                        MongoLanguageDirection(languageDirection)
                    )
                ),
                sample(1),
            )
        )
        .map(_.toCommonTranslation)
        .toOptionTask
  }

  private object Helpers {

    def convertUserData(maybeUserData: Option[MongoUserData]): Option[UserData] =
      maybeUserData.map { mongoUserData: MongoUserData =>
        UserData(
            chatId = mongoUserData.chatId,
            languageDirection = mongoUserData.languageDirection.map(convertLanguageDirection),
            decapitalization = mongoUserData.decapitalization,
        )
      }

    def convertLanguageDirection(mongoLanguageDirection: MongoLanguageDirection): LanguageDirection =
      LanguageDirection(
          source = mongoLanguageDirection.source,
          target = mongoLanguageDirection.target,
      )

  }

  private object Config {
    val username: String = config.getString("mongo.username")
    val password: String = config.getString("mongo.password")
    val host: String     = config.getString("mongo.host")
    val port: Int        = config.getInt("mongo.port")

    val connectionString = new ConnectionString(s"mongodb://$username:$password@$host:$port")

    val caseClassCodecs: CodecRegistry =
      fromProviders(
          classOf[MongoGlobals],
          classOf[MongoUserData],
          classOf[MongoCommonTranslation],
          classOf[MongoLanguageDirection],
          LanguageCodeCodecProvider
      )

    val codecRegistry: CodecRegistry =
      fromRegistries(DEFAULT_CODEC_REGISTRY, caseClassCodecs)

    val clientSettings: MongoClientSettings =
      MongoClientSettings
        .builder()
        .applyConnectionString(connectionString)
        .codecRegistry(codecRegistry)
        .build()

    val client = MongoClient(clientSettings)

    val emptyBson = BsonDocument()

    val upsert: UpdateOptions   = new UpdateOptions().upsert(true)
    val repsert: ReplaceOptions = new ReplaceOptions().upsert(true)

    object CollName {
      val globals   = "globals"
      val usersData = "users_data"
    }

    object DbName {
      val garcon = "garcon"
    }

  }

}

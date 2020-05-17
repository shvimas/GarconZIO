package dev.shvimas.garcon.database.mongo

import com.mongodb.ConnectionString
import dev.shvimas.garcon.{Database, MongoConfig}
import dev.shvimas.garcon.database.Database
import dev.shvimas.garcon.database.model._
import dev.shvimas.garcon.database.mongo.codec.LanguageCodeCodecProvider
import dev.shvimas.garcon.database.mongo.model._
import dev.shvimas.garcon.model.Text
import dev.shvimas.telegram.model.{Chat, Message}
import dev.shvimas.telegram.Bot
import dev.shvimas.translate.LanguageDirection
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{ReplaceOptions, UpdateOptions}
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import zio._

import scala.concurrent.Future

object Mongo {

  val live: ZLayer[Has[MongoConfig], Throwable, Database] = ZLayer.fromServiceM { config: MongoConfig =>
    ZIO.effect {
      val connectionString = {
        val username = config.username
        val password = config.password
        val host     = config.host
        val port     = config.port
        new ConnectionString(s"mongodb://$username:$password@$host:$port")
      }

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

      new Instance(MongoClient(clientSettings))
    }
  }

  private class Instance(client: MongoClient) extends Database.Service {

    import Constants._
    import Helpers._

    protected val garconDb: MongoDatabase = client.getDatabase(DbName.garcon)

    protected val globalsColl: MongoCollection[MongoGlobals] =
      garconDb.getCollection(CollName.globals)

    protected val usersDataColl: MongoCollection[MongoUserData] =
      garconDb.getCollection(CollName.usersData)

    def getGlobals: Task[Option[MongoGlobals]] =
      globalsColl
        .find()
        .first()
        .toOptionTask

    override def updateOffset(offset: Bot.Offset): Task[UpdateResult] =
      globalsColl
        .updateOne(
            filter = emptyBson,
            update = set(GlobalsFields.offset, offset.value),
            options = upsert
        )
        .toTask

    override def getOffset: Task[Bot.Offset] =
      getGlobals
        .map(_.flatMap(_.offset).getOrElse(0L))
        .map(Bot.Offset)

    private def getWordsColl(chatId: Chat.Id): MongoCollection[MongoCommonTranslation] =
      garconDb.getCollection(s"${chatId.value}_words")

    private def getTranslation(chatId: Chat.Id,
                               text: Text.Checked,
                               languageDirection: LanguageDirection,
    ): Task[Option[MongoCommonTranslation]] =
      getWordsColl(chatId)
        .find(
            filter = combine(
                equal(CommonTranslationFields.text, text.value),
                equal(
                    CommonTranslationFields.languageDirection,
                    MongoLanguageDirection(languageDirection)
                )
            )
        )
        .first()
        .toOptionTask

    override def lookUpText(text: Text.Checked,
                            languageDirection: LanguageDirection,
                            chatId: Chat.Id,
    ): Task[Option[CommonTranslation]] =
      getTranslation(chatId, text, languageDirection)
        .map(_.map(_.toCommonTranslation))

    override def addCommonTranslation(translation: CommonTranslation,
                                      chatId: Chat.Id,
                                      languageDirection: LanguageDirection,
                                      messageId: Message.Id,
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

    override def deleteText(text: Text.Checked, langDirection: LanguageDirection, chatId: Chat.Id): Task[DeleteResult] =
      getWordsColl(chatId)
        .deleteOne(
            filter = combine(
                equal(CommonTranslationFields.text, text.value),
                equal(
                    CommonTranslationFields.languageDirection,
                    MongoLanguageDirection(langDirection)
                )
            )
        )
        .toTask

    override def getUserData(chatId: Chat.Id): Task[Option[UserData]] =
      usersDataColl
        .find(filter = equal(UserDataFields.chatId, chatId.value))
        .first()
        .toOptionTask
        .map(convertUserData)

    override def setUserData(userData: UserData): Task[UpdateResult] =
      usersDataColl
        .replaceOne(
            filter = equal(UserDataFields.chatId, userData.chatId.value),
            replacement = MongoUserData(userData),
            repsert,
        )
        .toTask

    override def setLanguageDirection(chatId: Chat.Id, languageDirection: LanguageDirection): Task[UpdateResult] =
      usersDataColl
        .updateOne(
            equal(UserDataFields.chatId, chatId.value),
            set(UserDataFields.langDir, MongoLanguageDirection(languageDirection)),
            upsert
        )
        .toTask

    override def findLanguageDirectionForMessage(chatId: Chat.Id,
                                                 text: Text.Checked,
                                                 messageId: Message.Id,
    ): Task[Option[LanguageDirection]] =
      getWordsColl(chatId)
        .find(
            combine(
                equal(CommonTranslationFields.text, text.value),
                equal(CommonTranslationFields.messageId, messageId.value)
            )
        )
        .map(_.languageDirection.toLanguageDirection)
        .toOptionTask

    override def editTranslation(text: Text.Checked,
                                 edit: String,
                                 languageDirection: LanguageDirection,
                                 chatId: Chat.Id,
    ): Task[Option[UpdateResult]] =
      getTranslation(chatId, text, languageDirection).flatMap {
        case Some(mongoCommonTranslation) =>
          mongoCommonTranslation.messagedId match {
            case Some(messageId) =>
              val edited = mongoCommonTranslation.copy(edited = Some(edit)).toCommonTranslation
              addCommonTranslation(edited, chatId, languageDirection, Message.Id(messageId)).map(Some(_))
            case None => ZIO.none
          }
        case None => ZIO.none
      }

    /**
      * Returns randomly sampled words with needed language direction.
      * NB: sample may contain repeated elements
      * */
    override def getRandomWords(chatId: Chat.Id,
                                languageDirection: LanguageDirection,
                                numWords: Int): Task[Seq[CommonTranslation]] = {
      val filter: Bson = `match`(
          equal(
              CommonTranslationFields.languageDirection,
              MongoLanguageDirection(languageDirection)
          )
      )
      val pipeline = Seq(filter, sample(numWords))
      getWordsColl(chatId)
        .aggregate(pipeline)
        .map(_.toCommonTranslation)
        .toSeqTask
    }
  }

  private object Helpers {

    def fromFuture[A](future: => Future[A]): Task[A] =
      ZIO.fromFuture(implicit ec => future)

    implicit class RichSingleObservable[T](inner: SingleObservable[T]) {
      def toTask: Task[T]               = fromFuture(inner.toFuture())
      def toOptionTask: Task[Option[T]] = fromFuture(inner.toFutureOption())
    }

    implicit class RichObservable[T](inner: Observable[T]) {
      def toSeqTask: Task[Seq[T]]       = fromFuture(inner.toFuture())
      def toOptionTask: Task[Option[T]] = fromFuture(inner.headOption())
    }

    def convertUserData(maybeUserData: Option[MongoUserData]): Option[UserData] =
      maybeUserData.map { mongoUserData: MongoUserData =>
        UserData(
            chatId = Chat.Id(mongoUserData.chatId),
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

  private object Constants {
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

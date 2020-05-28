package dev.shvimas.garcon

import dev.shvimas.garcon.model._
import dev.shvimas.telegram._
import dev.shvimas.telegram.model._
import dev.shvimas.translate.LanguageDirection
import dev.shvimas.ZIOLogging
import dev.shvimas.garcon.database.mongo.Mongo
import dev.shvimas.garcon.database.DatabaseOps
import org.mongodb.scala.result.UpdateResult
import zio.{ZIO, _}
import zio.duration._

object Main extends App with ZIOLogging {

  import dev.shvimas.garcon.BotInteraction._
  import dev.shvimas.garcon.DatabaseInteraction._
  import dev.shvimas.garcon.TranslatorsInteraction._

  type GarconEnv = Has[Bot] with Database with Translators

  private def makeTelegramBot(config: TelegramBotConfig): ZIO[Has[TelegramBotConfig], Throwable, TelegramBot] =
    for {
      maybeProxy <- config.maybeProxy match {
        case Some(proxyConfig) => proxyConfig.makeSocksProxy.asSome
        case None              => zioLogger.warn("Telegram proxy settings not found!") *> ZIO.none
      }
      settings <- ZIO.effect(TelegramBotSettings(config.token, maybeProxy))
      bot      <- ZIO.effect(new TelegramBot(settings))
    } yield bot

  val bot: Layer[Throwable, Has[Bot]] = MainConfig.telegramBotConfig >>> ZLayer.fromServiceM(makeTelegramBot)

  val database: Layer[Throwable, Database] = MainConfig.mongoConfig >>> Mongo.live

  val translators: Layer[Throwable, Translators] = MainConfig.translatorsConfig >>> Translators.live

  val garconLayer: ULayer[GarconEnv] =
    (bot ++ database ++ translators)
      .logOnError("Failed to construct common layer from config")
      .orDie

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    main
      .provideLayer(garconLayer)
      .repeat(Schedule.fixed(200.milliseconds))
      .exitCode

  val main: ZIO[GarconEnv, Nothing, Unit] =
    for {
      maybeUpdatesResult <- getUpdates
      _ <- maybeUpdatesResult match {
        case Some(updatesResult) => updateOffset(updatesResult) <&> processUpdates(updatesResult)
        case None                => ZIO.unit
      }
    } yield ()

  def getUpdates: ZIO[Has[Bot] with Database, Nothing, Option[GetUpdatesResult]] = {
    val zGetUpdatesResult: ZIO[Has[Bot] with Database, Throwable, GetUpdatesResult] =
      for {
        offset <- DatabaseOps.getOffset
        result <- BotOps.getUpdates(offset)
      } yield result

    zGetUpdatesResult
      .logOnError("While getting updates")
      .option
  }

  def processUpdates(getUpdatesResult: GetUpdatesResult): ZIO[GarconEnv, Nothing, Unit] =
    for {
      groupedUpdates <- groupUpdates(getUpdatesResult)
      results        <- processGroupedUpdates(groupedUpdates)
      _              <- respondWith(results) <&> saveResults(results) <&> processErrors(results)
    } yield ()

  def groupUpdates(getUpdatesResult: GetUpdatesResult): UIO[Map[Chat.Id, List[Update]]] = {
    val zGroupedUpdates: Task[Map[Chat.Id, List[Update]]] =
      for {
        grouped <- ZIO.effect(getUpdatesResult.result.groupBy(_.chatId))
        processed <- ZIO.foreach(grouped) {
          case (Some(chatId), updates) => ZIO.some(chatId -> updates)
          case (None, updates)         => processOrphanUpdates(updates).as(None)
        }
      } yield processed.flatten.toMap
    zGroupedUpdates
      .logOnError("While grouping updates")
      .orElse(ZIO.succeed(Map.empty))
  }

  def respondWith(results: AllResults): ZIO[Has[Bot], Nothing, Unit] =
    for {
      sendResponsesResult <- sendResponses(results)
      _ <- ZIO.foreach(sendResponsesResult) {
        case (chatId, Left(throwable: Throwable)) =>
          zioLogger.error(s"Failed to send responses to $chatId", throwable)
        case (chatId, Right(sendMessageResults: List[SendMessageResult])) =>
          // TODO: resend if not ok?
          zioLogger.info(
              s"""Send message results for $chatId:
                 |${sendMessageResults.mkString("\n")}
                 |""".stripMargin
          )
      }
    } yield ()

  def processTranslationRequest(request: TranslationRequest): RIO[Database with Translators, TranslationResponse] =
    for {
      text                 <- Text.prepareText(request.text, request.chatId)
      languageDirection    <- resolveLangDirection(request.chatId)
      languageDirection    <- ZIO.effect(languageDirection.maybeReverse(text.value))
      commonTranslation    <- commonTranslation(text, languageDirection)
      maybePrevTranslation <- DatabaseOps.lookUpText(text, languageDirection, request.chatId)
      mergedTranslation = maybePrevTranslation.map(commonTranslation.mergeWith).getOrElse(commonTranslation)
    } yield
      TranslationResponse(
          TranslationWithInfo(
              translation = mergedTranslation,
              languageDirection = languageDirection,
              messageId = request.messageId,
          )
      )

  def processDeleteCommand(command: DeleteCommand): ZIO[Database, Throwable, DeletionResponse] = {
    def deleteText(text: Text.Checked,
                   languageDirection: LanguageDirection,
                   chatId: Chat.Id): ZIO[Database, Throwable, Either[String, String]] =
      DatabaseOps
        .deleteText(text, languageDirection, chatId)
        .map { result =>
          val textValue = text.value
          if (result.wasAcknowledged()) {
            Right(s"Deleted $textValue ($languageDirection)")
          } else {
            Left(s"Failed to delete $textValue ($languageDirection)")
          }
        }

    val result: ZIO[Database, Throwable, Either[String, String]] =
      command match {
        case DeleteByReply(reply, chatId) =>
          reply.text.map(Text.prepareText(_, chatId)) match {
            case Some(zText) =>
              for {
                text         <- zText
                maybeLangDir <- DatabaseOps.findLanguageDirectionForMessage(chatId, text, reply.messageId)
                result <- maybeLangDir match {
                  case Some(languageDirection) =>
                    deleteText(text, languageDirection, chatId)
                  case None =>
                    ZIO.succeed(Left(s"Couldn't delete ${text.value} (failed to find language direction)"))
                }
              } yield result
            case None =>
              ZIO.succeed(Left("Couldn't delete (text is empty)"))
          }
        case DeleteByText(text, languageDirection, chatId) =>
          for {
            checkedText <- Text.prepareText(text, chatId)
            response    <- deleteText(checkedText, languageDirection, chatId)
          } yield response
      }
    result.map(DeletionResponse)
  }

  def processEditCommand(command: EditCommand): ZIO[Database, Throwable, EditResponse] = {
    def editTranslation(text: Text.Checked,
                        edit: String,
                        chatId: Chat.Id,
                        languageDirection: LanguageDirection,
    ): ZIO[Database, Throwable, EditResponse] =
      DatabaseOps
        .editTranslation(text, edit, languageDirection, chatId)
        .map {
          case Some(updateResult) =>
            if (updateResult.wasAcknowledged()) {
              updateResult.getModifiedCount match {
                case 0 => FailedEditResponse("Nothing was modified in database")
                case 1 => SuccessfulEditResponse(text.value, languageDirection, edit)
                case n => FailedEditResponse(s"Database modified $n entries instead of one")
              }
            } else FailedEditResponse("Database request was not acknowledged")
          case None =>
            FailedEditResponse(s"Couldn't edit ${text.value} (failed to find translation)")
        }

    command match {
      case EditByReply(reply, edit, chatId) =>
        reply.text match {
          case Some(text) =>
            for {
              text         <- Text.prepareText(text, chatId)
              maybeLangDir <- DatabaseOps.findLanguageDirectionForMessage(chatId, text, reply.messageId)
              response <- maybeLangDir match {
                case Some(languageDirection) =>
                  editTranslation(text, edit, chatId, languageDirection)
                case None =>
                  ZIO.succeed(FailedEditResponse(s"Couldn't edit ${text.value} (failed to find language direction)"))
              }
            } yield response
          case None =>
            ZIO.succeed(FailedEditResponse("Couldn't edit (text is empty)"))
        }
    }
  }

  def processTestCommand(command: TestCommand): ZIO[Database, Throwable, TestResponse] =
    command match {
      case TestStartCommand(maybeLanguageDirection, chatId) =>
        val languageDirection = maybeLanguageDirection.getOrElse(Defaults.languageDirection)
        DatabaseOps
          .getRandomWord(chatId, languageDirection)
          .map(TestStartResponse(_, languageDirection))
      case TestNextCommand(languageDirection, chatId) =>
        DatabaseOps
          .getRandomWord(chatId, languageDirection)
          .map(TestNextResponse(_, languageDirection))
      case TestShowCommand(text, languageDirection, chatId) =>
        for {
          preparedText     <- Text.prepareText(text, chatId)
          maybeTranslation <- DatabaseOps.lookUpText(preparedText, languageDirection, chatId)
        } yield TestShowResponse(maybeTranslation, languageDirection)
    }

  def processChooseRequest(command: ChooseCommand): ZIO[Database, Throwable, ChooseResponse] =
    command match {
      case ChooseCommand(languageDirection, chatId) =>
        DatabaseOps
          .setLanguageDirection(chatId, languageDirection)
          .map { result: UpdateResult =>
            if (result.wasAcknowledged()) {
              SuccessfulChooseResponse(languageDirection)
            } else {
              FailedChooseResponse("Saving language direction was not acknowledged", languageDirection)
            }
          }
    }

  def processUpdate(update: Update): ZIO[Database with Translators, Throwable, Response] =
    RequestParser.parseUpdate(update).flatMap {
      case request: TranslationRequest  => processTranslationRequest(request)
      case command: DeleteCommand       => processDeleteCommand(command)
      case command: EditCommand         => processEditCommand(command)
      case command: TestCommand         => processTestCommand(command)
      case command: ChooseCommand       => processChooseRequest(command)
      case DecapitalizeCommand(state)   => ZIO.effect(DecapitalizeResponse(state))
      case HelpCommand                  => ZIO.effect(HelpResponse)
      case MalformedCommand(desc)       => ZIO.effect(MalformedCommandResponse(desc))
      case UnrecognisedCommand(command) => ZIO.effect(UnrecognisedCommandResponse(command))
      case EmptyUpdate                  => ZIO.effect(EmptyUpdateResponse)
      case EmptyMessage                 => ZIO.effect(EmptyMessageResponse)
      case EmptyCallbackData            => ZIO.effect(EmptyCallbackDataResponse)
      case BothMessageAndCallback       => ZIO.effect(BothMessageAndCallbackResponse(update))
    }

  case class ErrorWithInfo(error: Throwable, update: Update)

  type AllResults = List[(Chat.Id, List[Either[ErrorWithInfo, Response]])]

  def processGroupedUpdates(
      updateGroups: Map[Chat.Id, Seq[Update]],
  ): ZIO[Database with Translators, Nothing, AllResults] =
    // important that error type is Nothing in inner collect
    // otherwise collectAllPar could interrupt other users' processing
    ZIO.foreachPar(updateGroups) {
      case (chatId, updates) =>
        ZIO
          .foreach(updates) { update =>
            processUpdate(update)
              .mapError(ErrorWithInfo(_, update))
              .either
          }
          .map(chatId -> _)
    }

  private def processOrphanUpdates(orphanUpdates: Seq[Update]): UIO[Unit] =
    zioLogger.warn(
        s"""Got updates from unknown chat:
           |${orphanUpdates.mkString("\n")}
           |
           |
           |These updates were left unattended""".stripMargin
    )

  def processErrors(allResults: AllResults): ZIO[Any, Nothing, Unit] = {
    val errors: List[ErrorWithInfo] =
      allResults.flatMap {
        case (_, resultsPerUser) =>
          resultsPerUser.flatMap {
            case Left(errorWithInfo) => Some(errorWithInfo)
            case Right(_)            => None
          }
      }
    ZIO
      .foreach(errors) {
        case ErrorWithInfo(throwable, update) =>
          zioLogger.error(s"While processing $update", throwable)
      }
      .unit
  }

}

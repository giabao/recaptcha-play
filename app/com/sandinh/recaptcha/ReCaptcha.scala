package com.sandinh.recaptcha

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.ws.WSAPI
import play.api.mvc._
import play.api.http.Status.OK
import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal

@Singleton
class ReCaptcha @Inject() (val config: ReCaptchaConfig, wsApi: WSAPI) {
  import ReCaptcha._

  private val logger = Logger(getClass)

  private val ws = wsApi.url("https://www.google.com/recaptcha/api/siteverify")

  /**
   * Returns the most preferred language (as extracted from the <code>Accept-Language</code> HTTP header in the
   * request, if any) that is supported by reCAPTCHA. If no supported language is found, returns the default
   * language from configuration (if any), otherwise English ("en").
   * @param request		The web request
   * @return The language code
   */
  def getPreferredLanguage(implicit request: RequestHeader): String =
    request.acceptLanguages
        .find(l => config.supportedCodes.contains(l.language))
        .fold(config.defaultLanguage)(_.language)

  /**
   * Low level API (independent of Play form and request APIs).
   *
   * Verifies whether a recaptcha response is valid, by invoking the Google Recaptcha API version
   * 2 verify web service in a reactive manner.
   *
   * @param response		The recaptcha response, to verify
   * @param ec       Implicit - The execution context used for futures
   * @return A future of Some error code or None (if success)
   */
  def verify(response: String)(implicit request: RequestHeader, ec: ExecutionContext): Future[Option[String]] = {
    // create the v2 POST payload
    val payload = Map(
      "secret" -> Seq(config.privateKey),
      "response" -> Seq(response),
      "remoteip" -> Seq(request.remoteAddress)
    )

    ws.withRequestTimeout(config.requestTimeout)
      .post(payload)
      .map { res =>
        if (res.status == OK) {
          val js = res.json
          if ((js \ "success").as[Boolean]) {
            None
          } else {
            (js \ "error-codes").asOpt[Seq[String]] match {
              case None => Some("")
              case Some(errorCodes) => errorCodes.headOption
            }
          }
        } else {
          logger.error("Error calling recaptcha v2 API, HTTP response " + res.status)
          Some(ErrorCode.Error)
        }
      }.recover {
        case NonFatal(e) =>
          logger.error("Error calling recaptcha v2 API", e)
          Some(ErrorCode.Error)
      }
  }

  /** verify using `g-recaptcha-response` POST parameter when the user submits the form on your site */
  def verify()(implicit request: Request[_], ec: ExecutionContext): Future[Option[String]] = {
    val maybeResponse = for {
      maybeMap <- request.body match {
        case body: AnyContent => body.asFormUrlEncoded
        case _ => None
      }
      maybeResponses <- maybeMap.get(ResponseFieldKey)
      v <- maybeResponses.headOption
    } yield v

    maybeResponse match {
      case Some(v) => verify(v)
      case None => Future successful Some(ErrorCode.ResponseMissing)
    }
  }
}

object ReCaptcha {
  object ErrorCode {
    /** General error. */
    val Error = "recaptcha-error"

    /** The recaptcha response was missing from the request (probably an end user error). */
    val ResponseMissing = "missing-input-response"
  }

  /** The recaptcha (v2) response field name.
    * @see [[https://developers.google.com/recaptcha/docs/verify]] */
  val ResponseFieldKey = "g-recaptcha-response"
}
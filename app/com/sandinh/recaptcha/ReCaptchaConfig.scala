package com.sandinh.recaptcha

import javax.inject.{Inject, Singleton}
import play.api.Configuration

@Singleton
class ReCaptchaConfig @Inject() (config: Configuration) {
  val publicKey = config.getString("recaptcha.publicKey").get
  val privateKey = config.getString("recaptcha.privateKey").get

  val supportedCodes = config.getStringSeq("recaptcha.supportedCodes").get
  val defaultLanguage = config.getString("recaptcha.defaultLanguage").get
  val theme = config.getString("recaptcha.theme").get
  val tpe = config.getString("recaptcha.type").get
  val size = config.getString("recaptcha.size").get

  val requestTimeout = config.getMilliseconds("recaptcha.requestTimeout").get
}

/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.audit

import models.UserAnswers
import models.etmp.{EtmpDisplayRegistration, EtmpExclusionReason}
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Vrn

case class RegistrationAuditModel(
                                   registrationAuditType: RegistrationAuditType,
                                   userId: String,
                                   userAgent: String,
                                   vrn: String,
                                   iossNumber: String,
                                   userAnswers: UserAnswers,
                                   registration: EtmpDisplayRegistration,
                                   exclusionReason: Option[EtmpExclusionReason],
                                   submissionResult: SubmissionResult
                                 ) extends JsonAuditModel {

  override val auditType: String = registrationAuditType.auditType

  override val transactionName: String = registrationAuditType.transactionName

  override val detail: JsValue = Json.obj(
    "userId" -> userId,
    "browserUserAgent" -> userAgent,
    "requestersVrn" -> vrn,
    "iossNumber" -> iossNumber,
    "userAnswersDetails" -> Json.toJson(userAnswers),
    "registration" -> Json.toJson(registration),
    "exclusionReason" -> Json.toJson(exclusionReason),
    "submissionResult" -> submissionResult
  )
}

object RegistrationAuditModel {

  def build(
             registrationAuditType: RegistrationAuditType,
           userId: String,
             userAgent: String,
             vrn: Vrn,
             iossNumber: String,
           answers: UserAnswers,
           registration: EtmpDisplayRegistration,
             exclusionReason: Option[EtmpExclusionReason],
             submissionResult: SubmissionResult
           ): RegistrationAuditModel =
    RegistrationAuditModel(
      registrationAuditType = registrationAuditType,
      userId = userId,
      userAgent = userAgent,
      vrn = vrn.vrn,
      iossNumber = iossNumber,
      userAnswers = answers,
      registration = registration,
      exclusionReason = exclusionReason,
      submissionResult = submissionResult
    )

  def build(
             registrationAuditType: RegistrationAuditType,
             request: DataRequest[_],
             answers: UserAnswers,
             exclusionReason: Option[EtmpExclusionReason],
             submissionResult: SubmissionResult
           ): RegistrationAuditModel =
    RegistrationAuditModel(
      registrationAuditType = registrationAuditType,
      userId = request.userId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vrn = request.vrn.vrn,
      iossNumber = request.iossNumber,
      userAnswers = answers,
      registration = request.registrationWrapper.registration,
      exclusionReason = exclusionReason,
      submissionResult = submissionResult
    )

  def build(
             registrationAuditType: RegistrationAuditType,
             request: OptionalDataRequest[_],
             answers: UserAnswers,
             exclusionReason: Option[EtmpExclusionReason],
             submissionResult: SubmissionResult
           ): RegistrationAuditModel =
    RegistrationAuditModel(
      registrationAuditType = registrationAuditType,
      userId = request.userId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vrn = request.vrn.vrn,
      iossNumber = request.iossNumber,
      userAnswers = answers,
      registration = request.registrationWrapper.registration,
      exclusionReason = exclusionReason,
      submissionResult = submissionResult
    )
}

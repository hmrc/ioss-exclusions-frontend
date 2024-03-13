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

import models.UserAnswersForAudit
import models.etmp.{EtmpDisplayRegistration, EtmpExclusionReason}
import play.api.libs.json.{Json, JsValue}

case class ExclusionAuditModel(
                                exclusionAuditType: ExclusionAuditType,
                                userId: String,
                                userAgent: String,
                                vrn: String,
                                iossNumber: String,
                                userAnswers: UserAnswersForAudit,
                                registration: EtmpDisplayRegistration,
                                exclusionReason: Option[EtmpExclusionReason],
                                submissionResult: SubmissionResult
                                 ) extends JsonAuditModel {

  override val auditType: String = exclusionAuditType.auditType

  override val transactionName: String = exclusionAuditType.transactionName

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

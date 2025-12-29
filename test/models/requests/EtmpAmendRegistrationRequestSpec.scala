/*
 * Copyright 2024 HM Revenue & Customs
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

package models.requests

import base.SpecBase
import play.api.libs.json.*
import data.RegistrationData
import models.etmp.{EtmpAmendRegistrationChangeLog, EtmpAmendRegistrationChangeLogLegacy}
import org.scalacheck.Arbitrary.arbitrary

import java.time.LocalDate

class EtmpAmendRegistrationRequestSpec extends SpecBase with RegistrationData {

  private val administration = etmpAmendRegistrationRequest.administration
  private val customerIdentification = etmpAmendRegistrationRequest.customerIdentification
  private val tradingNames = etmpAmendRegistrationRequest.tradingNames
  private val schemeDetails = etmpAmendRegistrationRequest.schemeDetails
  private val bankDetails = etmpAmendRegistrationRequest.bankDetails
  private val changeLog = arbitrary[EtmpAmendRegistrationChangeLogLegacy].sample.value
  private val exclusionDetails = Some(EtmpExclusionDetailsLegacy(
    revertExclusion = true,
    noLongerSupplyGoods = false,
    exclusionRequestDate = Some(LocalDate.of(2024, 12, 16)),
    identificationValidityDate = Some(LocalDate.of(2024, 12, 15)),
    intExclusionRequestDate = Some(LocalDate.of(2024, 12, 14)),
    newMemberState = Some(
      EtmpNewMemberState(
        newMemberState = true,
        ceaseSpecialSchemeDate = Some(LocalDate.of(2024, 6, 1)),
        ceaseFixedEstDate = Some(LocalDate.of(2024, 5, 30)),
        movePOBDate = LocalDate.of(2024, 4, 1),
        issuedBy = "HMRC",
        vatNumber = "GB123456789"
      )
    )
  ))

  "EtmpAmendRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpAmendRegistrationRequest" in {

      val json = Json.obj(
        "administration" -> administration,
        "changeLog" -> changeLog,
        "exclusionDetails" -> exclusionDetails,
        "customerIdentification" -> customerIdentification,
        "tradingNames" -> tradingNames,
        "schemeDetails" -> schemeDetails,
        "bankDetails" -> bankDetails
      )

      val expectedResult = EtmpAmendRegistrationRequest(
        administration = administration,
        changeLog = changeLog,
        exclusionDetails = exclusionDetails,
        customerIdentification = customerIdentification,
        tradingNames = tradingNames,
        schemeDetails = schemeDetails,
        bankDetails = bankDetails
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpAmendRegistrationRequest] mustBe JsSuccess(expectedResult)
    }
  }
}

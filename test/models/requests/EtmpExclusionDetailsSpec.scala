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

import java.time.LocalDate

class EtmpExclusionDetailsSpec extends SpecBase {

  "EtmpExclusionDetails" - {

    "serialize and deserialize correctly when all fields are provided" in {

      val exclusionDetails = EtmpExclusionDetailsLegacy(
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
      )

      val json = Json.toJson(exclusionDetails)

      val expectedJson = Json.obj(
        "revertExclusion" -> true,
        "noLongerSupplyGoods" -> false,
        "partyType" -> "NETP",
        "exclusionRequestDate" -> "2024-12-16",
        "identificationValidityDate" -> "2024-12-15",
        "intExclusionRequestDate" -> "2024-12-14",
        "newMemberState" -> Json.obj(
          "newMemberState" -> true,
          "ceaseSpecialSchemeDate" -> "2024-06-01",
          "ceaseFixedEstDate" -> "2024-05-30",
          "movePOBDate" -> "2024-04-01",
          "issuedBy" -> "HMRC",
          "vatNumber" -> "GB123456789"
        )
      )

      json mustBe expectedJson

      json.as[EtmpExclusionDetails] mustBe exclusionDetails
    }

    "serialize and deserialize correctly with optional fields missing" in {

      val exclusionDetails = EtmpExclusionDetailsLegacy(
        revertExclusion = true,
        noLongerSupplyGoods = false,
        exclusionRequestDate = None,
        identificationValidityDate = None,
        intExclusionRequestDate = None,
        newMemberState = None
      )

      val json = Json.toJson(exclusionDetails)

      val expectedJson = Json.obj(
        "revertExclusion" -> true,
        "noLongerSupplyGoods" -> false,
        "partyType" -> "NETP"
      )

      json mustBe expectedJson

      json.as[EtmpExclusionDetails] mustBe exclusionDetails
    }

    "fail deserialization with invalid field types" in {
      val invalidJson = Json.obj(
        "revertExclusion" -> "true",
        "noLongerSupplyGoods" -> false,
        "partyType" -> "NETP"
      )

      invalidJson.validate[EtmpExclusionDetails].isError mustBe true
    }
  }

  "EtmpNewMemberState" - {

    "serialize and deserialize correctly when all fields are provided" in {
      val newMemberState = EtmpNewMemberState(
        newMemberState = true,
        ceaseSpecialSchemeDate = Some(LocalDate.of(2024, 6, 1)),
        ceaseFixedEstDate = Some(LocalDate.of(2024, 5, 30)),
        movePOBDate = LocalDate.of(2024, 4, 1),
        issuedBy = "HMRC",
        vatNumber = "GB123456789"
      )

      val json = Json.toJson(newMemberState)

      val expectedJson = Json.obj(
        "newMemberState" -> true,
        "ceaseSpecialSchemeDate" -> "2024-06-01",
        "ceaseFixedEstDate" -> "2024-05-30",
        "movePOBDate" -> "2024-04-01",
        "issuedBy" -> "HMRC",
        "vatNumber" -> "GB123456789"
      )

      json mustBe expectedJson

      json.as[EtmpNewMemberState] mustBe newMemberState
    }

    "serialize and deserialize correctly with optional fields missing" in {

      val newMemberState = EtmpNewMemberState(
        newMemberState = true,
        ceaseSpecialSchemeDate = None,
        ceaseFixedEstDate = None,
        movePOBDate = LocalDate.of(2024, 4, 1),
        issuedBy = "HMRC",
        vatNumber = "GB123456789"
      )

      val json = Json.toJson(newMemberState)

      val expectedJson = Json.obj(
        "newMemberState" -> true,
        "movePOBDate" -> "2024-04-01",
        "issuedBy" -> "HMRC",
        "vatNumber" -> "GB123456789"
      )

      json mustBe expectedJson

      json.as[EtmpNewMemberState] mustBe newMemberState
    }

    "fail deserialization with invalid field types" in {
      val invalidJson = Json.obj(
        "newMemberState" -> "true",
        "movePOBDate" -> "2024-04-01",
        "issuedBy" -> "HMRC",
        "vatNumber" -> "GB123456789"
      )

      invalidJson.validate[EtmpNewMemberState].isError mustBe true
    }
  }
}

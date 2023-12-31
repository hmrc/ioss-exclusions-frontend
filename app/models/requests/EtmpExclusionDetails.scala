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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class EtmpExclusionDetails(
                                 revertExclusion: Boolean,
                                 noLongerSupplyGoods: Boolean,
                                 partyType: String = "NETP",
                                 exclusionRequestDate: Option[LocalDate],
                                 identificationValidityDate: Option[LocalDate],
                                 intExclusionRequestDate: Option[LocalDate],
                                 newMemberState: Option[EtmpNewMemberState]
                               )

object EtmpExclusionDetails {

  implicit val format: OFormat[EtmpExclusionDetails] = Json.format[EtmpExclusionDetails]

}

case class EtmpNewMemberState(
                               newMemberState: Boolean,
                               ceaseSpecialSchemeDate: Option[LocalDate],
                               ceaseFixedEstDate: Option[LocalDate],
                               movePOBDate: LocalDate,
                               issuedBy: String,
                               vatNumber: String
                             )

object EtmpNewMemberState {

  implicit val format: OFormat[EtmpNewMemberState] = Json.format[EtmpNewMemberState]

}
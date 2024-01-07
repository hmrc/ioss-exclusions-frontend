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

package services

import connectors.RegistrationConnector
import connectors.RegistrationHttpParser.AmendRegistrationResultResponse
import models.{RegistrationWrapper, UserAnswers}
import models.etmp._
import models.requests.{EtmpAmendRegistrationRequest, EtmpExclusionDetails, EtmpNewMemberState}
import pages.{EuCountryPage, MoveDatePage, StoppedSellingGoodsDatePage, StoppedUsingServiceDatePage, TaxNumberPage}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector
                                   )(implicit ec: ExecutionContext) {

  def amendRegistration(
                         answers: UserAnswers,
                         exclusionReason: Option[EtmpExclusionReason],
                         vrn: Vrn,
                         registrationWrapper: RegistrationWrapper
                       )(implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] = {
    for {
      amend <- registrationConnector.amend(buildEtmpAmendRegistrationRequest(
        answers,
        exclusionReason,
        registrationWrapper.registration,
        vrn
      ))
    } yield amend

  }

  def buildEtmpAmendRegistrationRequest(
                                         answers: UserAnswers,
                                         exclusionReason: Option[EtmpExclusionReason],
                                         registration: EtmpDisplayRegistration,
                                         vrn: Vrn
                                       ): EtmpAmendRegistrationRequest = {

    EtmpAmendRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSSubscriptionAmend),
      changeLog = EtmpAmendRegistrationChangeLog(
        tradingNames = false,
        fixedEstablishments = false,
        contactDetails = false,
        bankDetails = false,
        reRegistration = exclusionReason.isEmpty
      ),
      exclusionDetails = exclusionReason.map(getExclusionDetailsForType(_, answers)),
      customerIdentification = EtmpCustomerIdentification(vrn),
      tradingNames = registration.tradingNames,
      schemeDetails = registration.schemeDetails,
      bankDetails = registration.bankDetails
    )
  }

  private def getExclusionDetailsForType(exclusionReason: EtmpExclusionReason, answers: UserAnswers): EtmpExclusionDetails = {
    exclusionReason match {
      case EtmpExclusionReason.TransferringMSID => getExclusionDetailsForTransferringMSID(answers)
      case EtmpExclusionReason.NoLongerSupplies => getExclusionDetailsForNoLongerSupplies(answers)
      case EtmpExclusionReason.VoluntarilyLeaves => getExclusionDetailsForVoluntarilyLeaves(answers)
      case _ => throw new Exception("Exclusion reason not valid")
    }
  }

  private def getExclusionDetailsForTransferringMSID(answers: UserAnswers): EtmpExclusionDetails = {
    val country = answers.get(EuCountryPage).getOrElse(throw new Exception("No country provided"))
    val moveDate = answers.get(MoveDatePage).getOrElse(throw new Exception("No move date provided"))
    val taxNumber = answers.get(TaxNumberPage).getOrElse(throw new Exception("No tax number provided"))

    EtmpExclusionDetails(
      revertExclusion = false,
      noLongerSupplyGoods = false,
      exclusionRequestDate = Some(LocalDate.now(clock)),
      identificationValidityDate = None,
      intExclusionRequestDate = Some(LocalDate.now(clock)),
      newMemberState = Some(EtmpNewMemberState(
        newMemberState = true,
        ceaseSpecialSchemeDate = None,
        ceaseFixedEstDate = None,
        movePOBDate = moveDate,
        issuedBy = country.code,
        vatNumber = taxNumber
      ))
    )
  }

  private def getExclusionDetailsForNoLongerSupplies(answers: UserAnswers): EtmpExclusionDetails = {
    val stoppedSellingGoodsDate = answers.get(StoppedSellingGoodsDatePage).getOrElse(throw new Exception("No stopped selling goods date provided"))
    EtmpExclusionDetails(
      revertExclusion = false,
      noLongerSupplyGoods = true,
      exclusionRequestDate = Some(stoppedSellingGoodsDate),
      identificationValidityDate = None,
      intExclusionRequestDate = Some(LocalDate.now(clock)),
      newMemberState = None
    )
  }

  private def getExclusionDetailsForVoluntarilyLeaves(answers: UserAnswers): EtmpExclusionDetails = {
    val stoppedUsingServiceDate = answers.get(StoppedUsingServiceDatePage).getOrElse(throw new Exception("No stopped using service date provided"))
    EtmpExclusionDetails(
      revertExclusion = false,
      noLongerSupplyGoods = false,
      exclusionRequestDate = Some(stoppedUsingServiceDate),
      identificationValidityDate = None,
      intExclusionRequestDate = Some(LocalDate.now(clock)),
      newMemberState = None
    )
  }

}

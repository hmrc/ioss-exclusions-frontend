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
import models.audit.{RegistrationAuditModel, RegistrationAuditType, SubmissionResult}
import models.etmp._
import models.requests.{EtmpAmendRegistrationRequest, EtmpExclusionDetails, EtmpNewMemberState}
import models.{CountryWithValidationDetails, UserAnswers}
import pages.{EuCountryPage, EuVatNumberPage, MoveDatePage, StoppedSellingGoodsDatePage, StoppedUsingServiceDatePage}
import play.api.mvc.Request
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector,
                                     auditService: AuditService
                                   )(implicit ec: ExecutionContext) {

  def amendRegistrationAndAudit(
                                 userId: String,
                                 vrn: Vrn,
                                 iossNumber: String,
                                 answers: UserAnswers,
                                 registration: EtmpDisplayRegistration,
                                 exclusionReason: Option[EtmpExclusionReason]
                               )(implicit hc: HeaderCarrier, request: Request[_]): Future[AmendRegistrationResultResponse] = {

    val success: RegistrationAuditModel = RegistrationAuditModel(
      registrationAuditType = RegistrationAuditType.AmendRegistration,
      userId = userId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vrn = vrn.vrn,
      iossNumber = iossNumber,
      userAnswers = answers,
      registration = registration,
      exclusionReason = exclusionReason,
      submissionResult = SubmissionResult.Success
    )
    val failure: RegistrationAuditModel = success.copy(submissionResult = SubmissionResult.Failure)

    amendRegistration(answers, exclusionReason, vrn, iossNumber, registration).andThen {
      case Success(Right(_)) => auditService.audit(success)(hc, request)
      case _ => auditService.audit(failure)(hc, request)
    }
  }

  private def amendRegistration(
                                 answers: UserAnswers,
                                 exclusionReason: Option[EtmpExclusionReason],
                                 vrn: Vrn,
                                 iossNumber: String,
                                 registration: EtmpDisplayRegistration
                               )(implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] = {
    registrationConnector.amend(buildEtmpAmendRegistrationRequest(
      answers,
      exclusionReason,
      registration,
      vrn,
      iossNumber
    ))
  }

  private def buildEtmpAmendRegistrationRequest(
                                                 answers: UserAnswers,
                                                 exclusionReason: Option[EtmpExclusionReason],
                                                 registration: EtmpDisplayRegistration,
                                                 vrn: Vrn,
                                                 iossNumber: String,
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
      customerIdentification = EtmpAmendCustomerIdentification(iossNumber),
      tradingNames = registration.tradingNames,
      schemeDetails = buildSchemeDetailsFromDisplay(registration.schemeDetails),
      bankDetails = registration.bankDetails
    )
  }

  private def buildSchemeDetailsFromDisplay(etmpDisplaySchemeDetails: EtmpDisplaySchemeDetails): EtmpSchemeDetails = {
    EtmpSchemeDetails(
      commencementDate = LocalDate.parse(etmpDisplaySchemeDetails.commencementDate),
      euRegistrationDetails = etmpDisplaySchemeDetails.euRegistrationDetails.map(buildEuRegistrationDetails),
      previousEURegistrationDetails = etmpDisplaySchemeDetails.previousEURegistrationDetails,
      websites = etmpDisplaySchemeDetails.websites,
      contactName = etmpDisplaySchemeDetails.contactName,
      businessTelephoneNumber = etmpDisplaySchemeDetails.businessTelephoneNumber,
      businessEmailId = etmpDisplaySchemeDetails.businessEmailId,
      nonCompliantReturns = etmpDisplaySchemeDetails.nonCompliantReturns,
      nonCompliantPayments = etmpDisplaySchemeDetails.nonCompliantPayments
    )
  }

  private def buildEuRegistrationDetails(euDisplayRegistrationDetails: EtmpDisplayEuRegistrationDetails): EtmpEuRegistrationDetails = {
    EtmpEuRegistrationDetails(
      countryOfRegistration = euDisplayRegistrationDetails.issuedBy,
      traderId = buildTraderId(euDisplayRegistrationDetails.vatNumber, euDisplayRegistrationDetails.taxIdentificationNumber),
      tradingName = euDisplayRegistrationDetails.fixedEstablishmentTradingName,
      fixedEstablishmentAddressLine1 = euDisplayRegistrationDetails.fixedEstablishmentAddressLine1,
      fixedEstablishmentAddressLine2 = euDisplayRegistrationDetails.fixedEstablishmentAddressLine2,
      townOrCity = euDisplayRegistrationDetails.townOrCity,
      regionOrState = euDisplayRegistrationDetails.regionOrState,
      postcode = euDisplayRegistrationDetails.postcode
    )
  }

  private def buildTraderId(maybeVatNumber: Option[String], maybeTaxIdentificationNumber: Option[String]): TraderId = {
    (maybeVatNumber, maybeTaxIdentificationNumber) match {
      case (Some(vatNumber), _) => VatNumberTraderId(vatNumber)
      case (_, Some(taxIdentificationNumber)) => TaxRefTraderID(taxIdentificationNumber)
      case _ => throw new IllegalStateException("Neither vat number nor tax id were provided")
    }
  }

  private def getExclusionDetailsForType(exclusionReason: EtmpExclusionReason, answers: UserAnswers): EtmpExclusionDetails = {
    exclusionReason match {
      case EtmpExclusionReason.TransferringMSID => getExclusionDetailsForTransferringMSID(answers)
      case EtmpExclusionReason.NoLongerSupplies => getExclusionDetailsForNoLongerSupplies(answers)
      case EtmpExclusionReason.VoluntarilyLeaves => getExclusionDetailsForVoluntarilyLeaves(answers)
      case EtmpExclusionReason.Reversal => getExclusionDetailsForReversal()
      case _ => throw new Exception("Exclusion reason not valid")
    }
  }

  private def getExclusionDetailsForTransferringMSID(answers: UserAnswers): EtmpExclusionDetails = {
    val country = answers.get(EuCountryPage).getOrElse(throw new Exception("No country provided"))
    val moveDate = answers.get(MoveDatePage).getOrElse(throw new Exception("No move date provided"))
    val euVatNumber = answers.get(EuVatNumberPage).getOrElse(throw new Exception("No VAT number provided"))
    val convertedVatNumber = CountryWithValidationDetails.convertTaxIdentifierForTransfer(euVatNumber, country.code)

    EtmpExclusionDetails(
      revertExclusion = false,
      noLongerSupplyGoods = false,
      exclusionRequestDate = Some(LocalDate.now(clock)),
      identificationValidityDate = None,
      intExclusionRequestDate = None,
      newMemberState = Some(EtmpNewMemberState(
        newMemberState = true,
        ceaseSpecialSchemeDate = None,
        ceaseFixedEstDate = None,
        movePOBDate = moveDate,
        issuedBy = country.code,
        vatNumber = convertedVatNumber
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

  private def getExclusionDetailsForReversal(): EtmpExclusionDetails = {
    EtmpExclusionDetails(
      revertExclusion = true,
      noLongerSupplyGoods = false,
      exclusionRequestDate = Some(LocalDate.now(clock)),
      identificationValidityDate = None,
      intExclusionRequestDate = Some(LocalDate.now(clock)),
      newMemberState = None
    )
  }

}

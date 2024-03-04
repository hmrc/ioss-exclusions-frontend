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

import base.SpecBase
import connectors.RegistrationConnector
import data.RegistrationData
import models.CountryWithValidationDetails
import models.audit.ExclusionAuditType
import models.etmp._
import models.requests.{EtmpExclusionDetails, EtmpNewMemberState}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import pages.{StoppedSellingGoodsDatePage, StoppedUsingServiceDatePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationServiceSpec extends SpecBase with BeforeAndAfterEach with RegistrationData {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAuditService: AuditService = mock[AuditService]
  private val registrationService = new RegistrationService(stubClock, mockRegistrationConnector, mockAuditService)

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockAuditService)
  }

  private def buildExpectedAmendRequest(etmpChangeLog: EtmpAmendRegistrationChangeLog, etmpExclusionsDetails: EtmpExclusionDetails) = {
    etmpAmendRegistrationRequest.copy(
      changeLog = etmpChangeLog,
      exclusionDetails = Some(etmpExclusionsDetails),
      customerIdentification = EtmpAmendCustomerIdentification(iossNumber),
      tradingNames = registrationWrapper.registration.tradingNames,
      schemeDetails = buildSchemeDetailsFromDisplay(registrationWrapper.registration.schemeDetails),
      bankDetails = registrationWrapper.registration.bankDetails
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

  ".amendRegistration" - {

    "when transferring of MSID" - {

      "must create registration request and return a successful ETMP enrolment response" in {

        val amendRegistrationResponse =
          Right(())

        val expectedChangeLog = EtmpAmendRegistrationChangeLog(
          tradingNames = false,
          fixedEstablishments = false,
          contactDetails = false,
          bankDetails = false,
          reRegistration = false
        )

        val convertedVatNumber = CountryWithValidationDetails.convertTaxIdentifierForTransfer(euVatNumber, country.code)

        val expectedExclusionDetails = EtmpExclusionDetails(
          revertExclusion = false,
          noLongerSupplyGoods = false,
          exclusionRequestDate = Some(LocalDate.now),
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

        val expectedAmendRegistrationRequest = buildExpectedAmendRequest(expectedChangeLog, expectedExclusionDetails)

        when(mockRegistrationConnector.amend(any())(any())) thenReturn amendRegistrationResponse.toFuture

        val app = applicationBuilder()
          .build()

        implicit val request = FakeRequest()

        running(app) {

          registrationService.amendRegistrationAndAudit(
            userAnswersId,
            vrn,
            iossNumber,
            completeUserAnswers,
            registrationWrapper.registration,
            Some(EtmpExclusionReason.TransferringMSID),
            ExclusionAuditType.ExclusionRequestSubmitted
          ).futureValue mustBe amendRegistrationResponse
          verify(mockRegistrationConnector, times(1)).amend(eqTo(expectedAmendRegistrationRequest))(any())
        }
      }
    }

    "when no longer supplying goods" - {

      "must create registration request and return a successful ETMP enrolment response" in {

        val amendRegistrationResponse =
          Right(())

        val expectedChangeLog = EtmpAmendRegistrationChangeLog(
          tradingNames = false,
          fixedEstablishments = false,
          contactDetails = false,
          bankDetails = false,
          reRegistration = false
        )

        val stoppedSellingGoodsDate = LocalDate.of(2023, 10, 5)

        val expectedExclusionDetails = EtmpExclusionDetails(
          revertExclusion = false,
          noLongerSupplyGoods = true,
          exclusionRequestDate = Some(stoppedSellingGoodsDate),
          identificationValidityDate = None,
          intExclusionRequestDate = None,
          newMemberState = None
        )

        val userAnswers = emptyUserAnswers
          .set(StoppedSellingGoodsDatePage, stoppedSellingGoodsDate).success.value

        val exceptedAmendRegistrationRequest = buildExpectedAmendRequest(expectedChangeLog, expectedExclusionDetails)

        when(mockRegistrationConnector.amend(any())(any())) thenReturn amendRegistrationResponse.toFuture

        val app = applicationBuilder()
          .build()

        implicit val request = FakeRequest()

        running(app) {

          registrationService.amendRegistrationAndAudit(
            userAnswersId,
            vrn,
            iossNumber,
            userAnswers,
            registrationWrapper.registration,
            Some(EtmpExclusionReason.NoLongerSupplies),
            ExclusionAuditType.ExclusionRequestSubmitted
          ).futureValue mustBe amendRegistrationResponse
          verify(mockRegistrationConnector, times(1)).amend(eqTo(exceptedAmendRegistrationRequest))(any())
        }
      }
    }

    "when voluntarily leaves" - {

      "must create registration request and return a successful ETMP enrolment response" in {

        val amendRegistrationResponse =
          Right(())

        val expectedChangeLog = EtmpAmendRegistrationChangeLog(
          tradingNames = false,
          fixedEstablishments = false,
          contactDetails = false,
          bankDetails = false,
          reRegistration = false
        )

        val stoppedUsingServiceDate = LocalDate.of(2023, 10, 4)

        val expectedExclusionDetails = EtmpExclusionDetails(
          revertExclusion = false,
          noLongerSupplyGoods = false,
          exclusionRequestDate = Some(stoppedUsingServiceDate),
          identificationValidityDate = None,
          intExclusionRequestDate = None,
          newMemberState = None
        )

        val userAnswers = emptyUserAnswers
          .set(StoppedUsingServiceDatePage, stoppedUsingServiceDate).success.value

        val exceptedAmendRegistrationRequest = buildExpectedAmendRequest(expectedChangeLog, expectedExclusionDetails)

        when(mockRegistrationConnector.amend(any())(any())) thenReturn amendRegistrationResponse.toFuture

        val app = applicationBuilder()
          .build()

        implicit val request = FakeRequest()

        running(app) {

          registrationService.amendRegistrationAndAudit(
            userAnswersId,
            vrn,
            iossNumber,
            userAnswers,
            registrationWrapper.registration,
            Some(EtmpExclusionReason.VoluntarilyLeaves),
            ExclusionAuditType.ExclusionRequestSubmitted
          ).futureValue mustBe amendRegistrationResponse
          verify(mockRegistrationConnector, times(1)).amend(eqTo(exceptedAmendRegistrationRequest))(any())
        }
      }
    }
  }

}

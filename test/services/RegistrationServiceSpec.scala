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
import models.etmp.{EtmpAmendRegistrationChangeLog, EtmpCustomerIdentification, EtmpExclusionReason}
import models.requests.{EtmpExclusionDetails, EtmpNewMemberState}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import pages.{StoppedSellingGoodsDatePage, StoppedUsingServiceDatePage}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationServiceSpec extends SpecBase with BeforeAndAfterEach with RegistrationData {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationService = new RegistrationService(stubClock, mockRegistrationConnector)

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  private def buildExpectedAmendRequest(etmpChangeLog: EtmpAmendRegistrationChangeLog, etmpExclusionsDetails: EtmpExclusionDetails) = {
    etmpAmendRegistrationRequest.copy(
      changeLog = etmpChangeLog,
      exclusionDetails = Some(etmpExclusionsDetails),
      customerIdentification = EtmpCustomerIdentification(vrn),
      tradingNames = registrationWrapper.registration.tradingNames,
      schemeDetails = registrationWrapper.registration.schemeDetails,
      bankDetails = registrationWrapper.registration.bankDetails
    )
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

        val expectedExclusionDetails = EtmpExclusionDetails(
          revertExclusion = false,
          noLongerSupplyGoods = false,
          exclusionRequestDate = Some(LocalDate.now),
          identificationValidityDate = None,
          intExclusionRequestDate = Some(LocalDate.now),
          newMemberState = Some(EtmpNewMemberState(
            newMemberState = true,
            ceaseSpecialSchemeDate = None,
            ceaseFixedEstDate = None,
            movePOBDate = moveDate,
            issuedBy = country.code,
            vatNumber = taxNumber
          ))
        )

        val exceptedAmendRegistrationRequest = buildExpectedAmendRequest(expectedChangeLog, expectedExclusionDetails)

        when(mockRegistrationConnector.amend(any())(any())) thenReturn amendRegistrationResponse.toFuture

        val app = applicationBuilder()
          .build()

        running(app) {

          registrationService.amendRegistration(
            completeUserAnswers,
            Some(EtmpExclusionReason.TransferringMSID),
            vrn,
            registrationWrapper
          ).futureValue mustBe amendRegistrationResponse
          verify(mockRegistrationConnector, times(1)).amend(eqTo(exceptedAmendRegistrationRequest))(any())
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
          intExclusionRequestDate = Some(LocalDate.now),
          newMemberState = None
        )

        val userAnswers = emptyUserAnswers
          .set(StoppedSellingGoodsDatePage, stoppedSellingGoodsDate).success.value

        val exceptedAmendRegistrationRequest = buildExpectedAmendRequest(expectedChangeLog, expectedExclusionDetails)

        when(mockRegistrationConnector.amend(any())(any())) thenReturn amendRegistrationResponse.toFuture

        val app = applicationBuilder()
          .build()

        running(app) {

          registrationService.amendRegistration(
            userAnswers,
            Some(EtmpExclusionReason.NoLongerSupplies),
            vrn,
            registrationWrapper
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
          intExclusionRequestDate = Some(LocalDate.now),
          newMemberState = None
        )

        val userAnswers = emptyUserAnswers
          .set(StoppedUsingServiceDatePage, stoppedUsingServiceDate).success.value

        val exceptedAmendRegistrationRequest = buildExpectedAmendRequest(expectedChangeLog, expectedExclusionDetails)

        when(mockRegistrationConnector.amend(any())(any())) thenReturn amendRegistrationResponse.toFuture

        val app = applicationBuilder()
          .build()

        running(app) {

          registrationService.amendRegistration(
            userAnswers,
            Some(EtmpExclusionReason.VoluntarilyLeaves),
            vrn,
            registrationWrapper
          ).futureValue mustBe amendRegistrationResponse
          verify(mockRegistrationConnector, times(1)).amend(eqTo(exceptedAmendRegistrationRequest))(any())
        }
      }
    }
  }

}

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
import data.RegistrationData.etmpAmendRegistrationRequest
import models.etmp.EtmpExclusionReason
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationService = new RegistrationService(stubClock, mockRegistrationConnector)

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  ".amendRegistration" - {

    "must create registration request and return a successful ETMP enrolment response" in {

      val amendRegistrationResponse =
        Right(())

      // TODO request per type
      when(mockRegistrationConnector.amend(etmpAmendRegistrationRequest)) thenReturn Right(amendRegistrationResponse).toFuture

      val app = applicationBuilder()
        .build()

      running(app) {

        registrationService.amendRegistration(completeUserAnswers, Some(EtmpExclusionReason.TransferringMSID), vrn).futureValue mustBe amendRegistrationResponse
        verify(mockRegistrationConnector, times(1)).amend(eqTo(etmpAmendRegistrationRequest))
      }
    }
  }

}

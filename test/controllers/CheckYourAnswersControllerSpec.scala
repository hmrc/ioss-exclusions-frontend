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

package controllers

import base.SpecBase
import connectors.RegistrationConnector
import models.CheckMode
import models.audit.{ExclusionAuditModel, ExclusionAuditType, SubmissionResult}
import models.etmp.EtmpExclusionReason
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import pages.{ApplicationCompletePage, CheckYourAnswersPage, EmptyWaypoints, EuCountryPage, EuVatNumberPage, MoveDatePage, Waypoint, Waypoints}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AuditService
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  val waypoints: Waypoints = EmptyWaypoints

  private val mockAuditService: AuditService = mock[AuditService]
  private val mockRegistrationConnector = mock[RegistrationConnector]

  override protected def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockAuditService)
  }

  "Check Your Answers Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]
          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
          val list = SummaryListViewModel(Seq.empty)

          status(result) mustBe OK

          contentAsString(result) mustBe view(waypoints, list, isValid = false)(request, messages(application)).toString
        }
      }
    }

    ".onSubmit" - {

      "must redirect to the correct page and audit a success event when the validation passes" in {

        when(mockRegistrationConnector.amend(any())(any())) thenReturn
          Future.successful(Right(()))

        val userAnswers = completeUserAnswers
        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)
          val result = route(application, request).value

          val expectedAuditEvent = ExclusionAuditModel(
            ExclusionAuditType.ExclusionRequestSubmitted,
            userAnswersId,
            "",
            vrn.vrn,
            iossNumber,
            userAnswers,
            registrationWrapper.registration,
            Some(EtmpExclusionReason.TransferringMSID),
            SubmissionResult.Success
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe ApplicationCompletePage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      "must redirect to the failure page and audit a failure event when the validation passes but amend call failures" in {

        when(mockRegistrationConnector.amend(any())(any())) thenReturn
          Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Error occurred")))

        val userAnswers = completeUserAnswers
        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          val expectedAuditEvent = ExclusionAuditModel(
            ExclusionAuditType.ExclusionRequestSubmitted,
            userAnswersId,
            "",
            vrn.vrn,
            iossNumber,
            userAnswers,
            registrationWrapper.registration,
            Some(EtmpExclusionReason.TransferringMSID),
            SubmissionResult.Failure
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.SubmissionFailureController.onPageLoad().url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      "when the user has not answered all necessary data" - {
        "the user is redirected when the incomplete prompt is shown" - {
          "to the Eu Country page when the EU country is missing" in {
            val answers = completeUserAnswers.remove(EuCountryPage).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe controllers.routes.EuCountryController.onPageLoad(waypoints).url
            }
          }

          "to the Move Date page when the move date is missing" in {
            val answers = completeUserAnswers.remove(MoveDatePage).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe controllers.routes.MoveDateController.onPageLoad(waypoints).url
            }
          }

          "to the EU VAT Number page when the VAT number is missing" in {
            val answers = completeUserAnswers.remove(EuVatNumberPage).success.value

            val application = applicationBuilder(userAnswers = Some(answers))
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe controllers.routes.EuVatNumberController.onPageLoad(waypoints).url
            }
          }
        }
      }
    }
  }
}

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

package controllers.reversal

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import forms.CancelLeaveSchemeFormProvider
import models.{RegistrationWrapper, UserAnswers}
import models.audit.{ExclusionAuditModel, ExclusionAuditType, SubmissionResult}
import models.etmp.{EtmpExclusion, EtmpExclusionReason}
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{CancelLeaveSchemeCompletePage, CancelLeaveSchemePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{AuditService, RegistrationService}
import views.html.CancelLeaveSchemeView

import java.time.LocalDate
import scala.concurrent.Future

class CancelLeaveSchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val formProvider = new CancelLeaveSchemeFormProvider()
  val form = formProvider()

  lazy val cancelLeaveSchemeRoute = routes.CancelLeaveSchemeController.onPageLoad(emptyWaypoints).url

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockAuditService: AuditService = mock[AuditService]

  val noLongerSuppliesExclusion = EtmpExclusion(
    NoLongerSupplies,
    LocalDate.now(stubClock).plusDays(2),
    LocalDate.now(stubClock).minusDays(1),
    false
  )

  val registrationNoLongerSuppliesExclusion: RegistrationWrapper =
    registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq(noLongerSuppliesExclusion)))

  override protected def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockAuditService)
  }

  "CancelLeaveScheme Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationNoLongerSuppliesExclusion
      ).build()

      running(application) {
        val request = FakeRequest(GET, cancelLeaveSchemeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CancelLeaveSchemeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(CancelLeaveSchemePage, true).success.value

      val application = applicationBuilder(
        userAnswers = Some(userAnswers),
        registration = registrationNoLongerSuppliesExclusion
      ).build()

      running(application) {
        val request = FakeRequest(GET, cancelLeaveSchemeRoute)

        val view = application.injector.instanceOf[CancelLeaveSchemeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to CancelLeaveSchemeCompletePage and audit a success when the user submits true and is cancelling their request to leave the scheme" in {

      when(mockRegistrationConnector.amend(any())(any())) thenReturn Future.successful(Right(()))

      val userAnswers = emptyUserAnswers
      val application = applicationBuilder(
        userAnswers = Some(userAnswers),
        registration = registrationNoLongerSuppliesExclusion
      ).overrides(
        bind[RegistrationConnector].toInstance(mockRegistrationConnector),
        bind[AuditService].toInstance(mockAuditService)
      ).build()

      running(application) {
        val request = FakeRequest(POST, cancelLeaveSchemeRoute).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAuditEvent = ExclusionAuditModel(
          ExclusionAuditType.ReversalRequestSubmitted,
          userAnswersId,
          "",
          vrn.vrn,
          iossNumber,
          userAnswers.set(CancelLeaveSchemePage, true).success.value.toUserAnswersForAudit,
          registrationNoLongerSuppliesExclusion.registration,
          Some(EtmpExclusionReason.Reversal),
          SubmissionResult.Success
        )

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CancelLeaveSchemeCompletePage.route(emptyWaypoints).url
        verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
      }
    }

    "must redirect to /your-account when the user submits false and is not cancelling their request to leave the scheme" in {
      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationNoLongerSuppliesExclusion
      ).build()

      running(application) {
        val request = FakeRequest(POST, cancelLeaveSchemeRoute).withFormUrlEncodedBody(("value", "false"))

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual config.iossYourAccountUrl
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationNoLongerSuppliesExclusion
      ).build()

      running(application) {
        val request = FakeRequest(POST, cancelLeaveSchemeRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CancelLeaveSchemeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Cancel Leave Scheme Error when a trader tries to reverse their exclusion and the effective date has passed" in {

      val noLongerSuppliesExclusion = EtmpExclusion(
        NoLongerSupplies,
        LocalDate.now(stubClock),
        LocalDate.now(stubClock).minusDays(1),
        false
      )

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq(noLongerSuppliesExclusion)))
      ).build()

      running(application) {

        val request = FakeRequest(GET, cancelLeaveSchemeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.CancelLeaveSchemeErrorController.onPageLoad().url
      }
    }

    "must return OK with default data if no existing data is found" in {

      val application = applicationBuilder(
        userAnswers = None,
        registration = registrationNoLongerSuppliesExclusion
      ).build()

      running(application) {
        val request = FakeRequest(GET, cancelLeaveSchemeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CancelLeaveSchemeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must call sessionRepository.clear on a GET" in {

      val mockSessionRepository: SessionRepository = mock[SessionRepository]
      val mockRegistrationService: RegistrationService = mock[RegistrationService]

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationNoLongerSuppliesExclusion
      ).overrides(
        bind[SessionRepository].toInstance(mockSessionRepository),
        bind[RegistrationService].toInstance(mockRegistrationService)
      ).build()

      running(application) {
        val request = FakeRequest(GET, cancelLeaveSchemeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
        verify(mockSessionRepository, times(1)).clear(any())
      }
    }
  }
}

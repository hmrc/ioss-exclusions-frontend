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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.CancelLeaveSchemeCompleteView

import scala.concurrent.Future

class CancelLeaveSchemeCompleteControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository)
  }

  "CancelLeaveSchemeComplete Controller" - {

    "must return OK and the correct view for a GET and clear the session" in {

      when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers)
      ).overrides(
        bind[SessionRepository].toInstance(mockSessionRepository)
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.CancelLeaveSchemeCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]
        val view = application.injector.instanceOf[CancelLeaveSchemeCompleteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(config.iossYourAccountUrl)(request, messages(application)).toString

        verify(mockSessionRepository, times(1)).clear(any())
      }
    }

    "must return OK and the correct view for a GET when no userAnswers are present" in {

      val application = applicationBuilder(
        userAnswers = None
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.CancelLeaveSchemeCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]
        val view = application.injector.instanceOf[CancelLeaveSchemeCompleteView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(config.iossYourAccountUrl)(request, messages(application)).toString
        
        verify(mockSessionRepository, never()).clear(any())
      }
    }
  }
}

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
import config.FrontendAppConfig
import forms.CancelLeaveSchemeFormProvider
import models.UserAnswers
import org.scalatestplus.mockito.MockitoSugar
import pages.{CancelLeaveSchemeCompletePage, CancelLeaveSchemePage}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.CancelLeaveSchemeView

class CancelLeaveSchemeControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new CancelLeaveSchemeFormProvider()
  val form = formProvider()

  lazy val cancelLeaveSchemeRoute = routes.CancelLeaveSchemeController.onPageLoad(emptyWaypoints).url

  "CancelLeaveScheme Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

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

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, cancelLeaveSchemeRoute)

        val view = application.injector.instanceOf[CancelLeaveSchemeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to CancelLeaveSchemeCompletePage when the user submits true and is cancelling their request to leave the scheme" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, cancelLeaveSchemeRoute).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CancelLeaveSchemeCompletePage.route(emptyWaypoints).url
      }
    }

    "must redirect to /your-account when the user submits false and is not cancelling their request to leave the scheme" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, cancelLeaveSchemeRoute).withFormUrlEncodedBody(("value", "false"))

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual config.iossYourAccountUrl
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, cancelLeaveSchemeRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CancelLeaveSchemeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, cancelLeaveSchemeRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, cancelLeaveSchemeRoute).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}

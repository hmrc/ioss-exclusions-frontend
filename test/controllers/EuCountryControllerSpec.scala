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
import forms.EuCountryFormProvider
import models.Country._
import models.UserAnswers
import pages.EuCountryPage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.EuCountryView

class EuCountryControllerSpec extends SpecBase {

  val formProvider = new EuCountryFormProvider()
  val form = formProvider()

  lazy val euCountryRoute = routes.EuCountryController.onPageLoad(emptyWaypoints).url

  "EuCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EuCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(EuCountryPage, country).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val view = application.injector.instanceOf[EuCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(country), emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, euCountryRoute).withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value

        val userAnswers = UserAnswers(userAnswersId).set(EuCountryPage, country).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuCountryPage.navigate(emptyWaypoints, emptyUserAnswers, userAnswers).url
      }
    }

    "must redirect to the tax number page when the user changes the country in check mode" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, euCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value

        val userAnswers = UserAnswers(userAnswersId).set(EuCountryPage, country).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual EuCountryPage.navigate(emptyWaypoints, emptyUserAnswers, userAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, euCountryRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EuCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, euCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, euCountryRoute).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}

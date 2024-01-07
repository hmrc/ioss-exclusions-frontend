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
import forms.MoveCountryFormProvider
import models.UserAnswers
import pages.MoveCountryPage
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.MoveCountryView

class MoveCountryControllerSpec extends SpecBase {

  val formProvider = new MoveCountryFormProvider()
  val form = formProvider()

  lazy val moveCountryRoute = routes.MoveCountryController.onPageLoad(emptyWaypoints).url

  "MoveCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, moveCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[MoveCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(MoveCountryPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, moveCountryRoute)

        val view = application.injector.instanceOf[MoveCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, moveCountryRoute).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val userAnswers = UserAnswers(userAnswersId).set(MoveCountryPage, true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual MoveCountryPage.navigate(emptyWaypoints, emptyUserAnswers, userAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, moveCountryRoute).withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[MoveCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must return OK with default data if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, moveCountryRoute)

        val result = route(application, request).value

         val view = application.injector.instanceOf[MoveCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, emptyWaypoints)(request, messages(application)).toString
      }
    }
  }
}

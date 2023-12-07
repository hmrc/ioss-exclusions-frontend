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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ApplicationCompleteView
import play.api.inject.bind

import java.time.{Clock, Instant, ZoneId}

class ApplicationCompleteControllerSpec extends SpecBase {

  "ExclusionsRequestReceivedConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {

      val clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).overrides(bind[Clock].toInstance(clock)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        status(result) mustEqual OK
        val dummyLeaveDate = "2 January 1970"
        val dummyCancelDate = "1 January 1970"
        contentAsString(result) mustEqual view(dummyLeaveDate, dummyCancelDate)(request, messages(application)).toString
      }
    }
  }
}
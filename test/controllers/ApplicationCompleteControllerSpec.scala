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
import date.Today
import org.mockito.MockitoSugar.when
import pages.{MoveCountryPage, StopSellingGoodsPage, StoppedUsingServiceDatePage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ApplicationCompleteView

import java.time.LocalDate

class ApplicationCompleteControllerSpec extends SpecBase {

  "ExclusionsRequestReceivedConfirmation Controller" - {

    "must return OK with the leave date and cancel date being 1st day of next month (1st Feb) when someone stops using the service " +
      "at least 15 days prior to the end of the month (16th Jan)" in {

      val today = LocalDate.of(2024, 1, 25)
      val mockToday = mock[Today]
      when(mockToday.date).thenReturn(today)

      val stoppedUsingServiceDate = LocalDate.of(2024, 1, 16)

      val userAnswers = emptyUserAnswers
        .set(MoveCountryPage, false).success.get
        .set(StopSellingGoodsPage, false).success.get
        .set(StoppedUsingServiceDatePage, stoppedUsingServiceDate).success.get

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[Today].toInstance(mockToday))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        val leaveDate = "1 February 2024"
        contentAsString(result) mustEqual view(config.iossYourAccountUrl, leaveDate, leaveDate)(request, messages(application)).toString
      }
    }

    "must return OK with the leave date and cancel date being 1st day of the following month (1st March) when someone stops using the service " +
      "less than 15 days prior to the end of the month (17th Jan)" in {

      val today = LocalDate.of(2024, 1, 25)
      val mockToday = mock[Today]
      when(mockToday.date).thenReturn(today)

      val stoppedUsingServiceDate = LocalDate.of(2024, 1, 17)

      val userAnswers = emptyUserAnswers
        .set(MoveCountryPage, false).success.get
        .set(StopSellingGoodsPage, false).success.get
        .set(StoppedUsingServiceDatePage, stoppedUsingServiceDate).success.get

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[Today].toInstance(mockToday))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        val leaveDate = "1 March 2024"
        contentAsString(result) mustEqual view(config.iossYourAccountUrl, leaveDate, leaveDate)(request, messages(application)).toString
      }
    }
  }
}

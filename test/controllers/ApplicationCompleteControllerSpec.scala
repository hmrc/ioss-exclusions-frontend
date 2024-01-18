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
import pages.{EuCountryPage, MoveCountryPage, MoveDatePage, StoppedSellingGoodsDatePage, StoppedUsingServiceDatePage, StopSellingGoodsPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ApplicationCompleteView

import java.time.LocalDate

class ApplicationCompleteControllerSpec extends SpecBase {

  val today = LocalDate.of(2024, 1, 25)
  val mockToday = mock[Today]
  when(mockToday.date).thenReturn(today)

  "ApplicationComplete Controller" - {

    "when someone moves business" - {
      "must return OK with the leave date being the 10th of next month (10th Feb)" in {
        val userAnswers = emptyUserAnswers
          .set(MoveCountryPage, true).success.get
          .set(EuCountryPage, country).success.get
          .set(MoveDatePage, today).success.get

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[Today].toInstance(mockToday))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ApplicationCompleteView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual OK
          val leaveDate = "25 January 2024"
          val maxMoveDate = "10 February 2024"

          contentAsString(result) mustEqual view(
            config.iossYourAccountUrl,
            leaveDate,
            maxMoveDate,
            Some(messages(application)("applicationComplete.moving.text", country.name)),
            Some(messages(application)("applicationComplete.next.info.bullet0", country.name, maxMoveDate))
          )(request, messages(application)).toString
        }
      }
    }

    "when someone stops selling goods" - {
      "must return OK with the leave date being end of the month (31st Jan) " +
        "when stopping at least 15 days prior to the end of the month (16th Jan)" in {

        val stoppedSellingGoodsDate = LocalDate.of(2024, 1, 16)

        val userAnswers = emptyUserAnswers
          .set(MoveCountryPage, false).success.get
          .set(StopSellingGoodsPage, true).success.get
          .set(StoppedSellingGoodsDatePage, stoppedSellingGoodsDate).success.get

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
          val maxChangeDate = "31 January 2024"

          contentAsString(result) mustEqual view(
            config.iossYourAccountUrl,
            leaveDate,
            maxChangeDate,
            Some(messages(application)("applicationComplete.stopSellingGoods.text"))
          )(request, messages(application)).toString
        }
      }
    }

    "when someone stops using the service" - {
      "must return OK with the leave date being 1st day of next month (1st Feb) " +
        "when stopping at least 15 days prior to the end of the month (16th Jan)" in {

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
          val maxChangeDate = "31 January 2024"
          contentAsString(result) mustEqual view(config.iossYourAccountUrl, leaveDate, maxChangeDate)(request, messages(application)).toString
        }
      }

      "must return OK with the leave date being 1st day of the following month (1st March) " +
        "when stopping less than 15 days prior to the end of the month (17th Jan)" in {

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
          val maxChangeDate = "29 February 2024"
          contentAsString(result) mustEqual view(config.iossYourAccountUrl, leaveDate, maxChangeDate)(request, messages(application)).toString
        }
      }
    }
  }

}

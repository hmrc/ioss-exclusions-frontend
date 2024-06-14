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

package controllers.actions

import base.SpecBase
import controllers.routes
import models.RegistrationWrapper
import models.requests.OptionalDataRequest
import pages.MoveCountryPage
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CannotAccessFilterSpec extends SpecBase {

  private val registrationWithNoExclusions: RegistrationWrapper =
    registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

  class Harness extends CannotAccessFilter {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  "CannotAccessFilter" - {

    "must redirect to Cannot Access Page when user is trying to access Check your answers when not permitted" in {

      val answers = emptyUserAnswers.set(MoveCountryPage, false).success.value

      val application = applicationBuilder().build()

      running(application) {

        val request = OptionalDataRequest(FakeRequest(), userAnswersId, Some(answers), vrn, iossNumber, registrationWithNoExclusions)

        val controller = new Harness()

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(routes.CannotAccessController.onPageLoad().url))
      }
    }

    "must return None when user is trying to access Check your answers when permitted" in {

      val answers = emptyUserAnswers.set(MoveCountryPage, true).success.value

      val application = applicationBuilder().build()

      running(application) {

        val request = OptionalDataRequest(FakeRequest(), userAnswersId, Some(answers), vrn, iossNumber, registrationWithNoExclusions)

        val controller = new Harness()

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }
  }
}

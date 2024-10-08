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
import date.Dates
import forms.StoppedUsingServiceDateFormProvider
import models.{RegistrationWrapper, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary
import org.scalatest.BeforeAndAfterEach
import pages.{EmptyWaypoints, StoppedUsingServiceDatePage}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.StoppedUsingServiceDateView

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class StoppedUsingServiceDateControllerSpec extends SpecBase with BeforeAndAfterEach {

  implicit val messages: Messages = stubMessages()

  val formProvider = new StoppedUsingServiceDateFormProvider()

  private def form(currentDate: LocalDate = LocalDate.now(), registrationDate: LocalDate = LocalDate.now()): Form[LocalDate] =
    formProvider.apply(currentDate, registrationDate)

  val validAnswer: LocalDate = LocalDate.now(ZoneOffset.UTC)

  lazy val stoppedUsingServiceDateRoute: String = routes.StoppedUsingServiceDateController.onPageLoad(EmptyWaypoints).url

  override val emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, stoppedUsingServiceDateRoute)


  def createPostPayload(day: String = validAnswer.getDayOfMonth.toString,
                        month: String = validAnswer.getMonthValue.toString,
                        year: String = validAnswer.getYear.toString): Map[String, String] = {
    Map(
      "value.day" -> day,
      "value.month" -> month,
      "value.year" -> year
    )
  }

  def postRequest(payload: Map[String, String] = createPostPayload()): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, stoppedUsingServiceDateRoute)
      .withFormUrlEncodedBody(
        payload.toList: _*
      )

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  "StoppedUsingServiceDate Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val result = route(application, getRequest()).value

        val view = application.injector.instanceOf[StoppedUsingServiceDateView]
        val dates = application.injector.instanceOf[Dates]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form(), dates.dateHint, emptyWaypoints)(getRequest(), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(StoppedUsingServiceDatePage, validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val view = application.injector.instanceOf[StoppedUsingServiceDateView]
        val dates = application.injector.instanceOf[Dates]

        val result = route(application, getRequest()).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form().fill(validAnswer), dates.dateHint, emptyWaypoints)(getRequest(), messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val registrationWrapper = Arbitrary.arbitrary[RegistrationWrapper].sample.value
      val etmpSchemeDetails = registrationWrapper.registration.schemeDetails
      val validDateWrapper = registrationWrapper.copy(
        registration = registrationWrapper.registration.copy(
          schemeDetails = etmpSchemeDetails.copy(commencementDate = LocalDate.now().toString))
      )

      when(mockRegistrationConnector.amend(any())(any())) thenReturn Future.successful(Right(()))

      val userAnswers = emptyUserAnswers
      val application = applicationBuilder(userAnswers = Some(userAnswers), registration = validDateWrapper)
        .overrides(
          bind[RegistrationConnector].toInstance(mockRegistrationConnector),
        )
        .build()

      running(application) {
        val result = route(application, postRequest()).value

        val userAnswers = UserAnswers(userAnswersId).set(StoppedUsingServiceDatePage, validAnswer).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual StoppedUsingServiceDatePage.navigate(emptyWaypoints, emptyUserAnswers, userAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid date values are submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()
      val payload = createPostPayload(day = "32")
      val request = postRequest(payload)

      running(application) {

        val view = application.injector.instanceOf[StoppedUsingServiceDateView]
        val dates = application.injector.instanceOf[Dates]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        val boundForm = form().bind(payload)
        contentAsString(result) mustEqual view(boundForm, dates.dateHint, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data structure is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val invalidDataStructure = ("value", "invalid value")
      val request =
        FakeRequest(POST, stoppedUsingServiceDateRoute)
          .withFormUrlEncodedBody(invalidDataStructure)

      running(application) {
        val view = application.injector.instanceOf[StoppedUsingServiceDateView]
        val dates = application.injector.instanceOf[Dates]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val boundForm = form().bind(Map(invalidDataStructure))
        contentAsString(result) mustEqual view(boundForm, dates.dateHint, emptyWaypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val result = route(application, getRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val result = route(application, postRequest()).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}

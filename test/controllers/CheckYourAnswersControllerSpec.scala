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
import date.{Dates, Today}
import connectors.RegistrationConnector
import models.audit.{ExclusionAuditModel, ExclusionAuditType, SubmissionResult}
import models.etmp.EtmpExclusionReason
import models.responses.UnexpectedResponseStatus
import models.{CheckMode, Country, RegistrationWrapper}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import pages.*
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AuditService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.checkAnswers.*
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach with PrivateMethodTester {

  val waypoints: Waypoints = EmptyWaypoints

  private val mockAuditService: AuditService = mock[AuditService]
  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val today: LocalDate = LocalDate.now
  private val mockToday: Today = mock[Today]
  when(mockToday.date).thenReturn(today)
  private val date: Dates = new Dates(mockToday)
  private val answers = emptyUserAnswers
    .set(MoveCountryPage, true).success.value
    .set(EuCountryPage, Country("DE", "Germany")).success.value
    .set(MoveDatePage, today).success.value
    .set(EuVatNumberPage, "DE123456789").success.value

  private val registration: RegistrationWrapper = registrationWrapper.copy(registration =
    registrationWrapper.registration
      .copy(exclusions = Seq.empty)
  )

  override protected def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockAuditService)
  }

  "Check Your Answers Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(answers), registration = registration)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val view = application.injector.instanceOf[CheckYourAnswersView]
          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
          val list = SummaryListViewModel(
            Seq(
              MoveCountrySummary.row(answers, waypoints, CheckYourAnswersPage),
              EuCountrySummary.rowNewCountry(answers, waypoints, CheckYourAnswersPage),
              MoveDateSummary.rowMoveDate(answers, waypoints, CheckYourAnswersPage, date),
              EuVatNumberSummary.rowEuVatNumber(answers, waypoints, CheckYourAnswersPage),
              StopSellingGoodsSummary.row(answers, waypoints, CheckYourAnswersPage),
              StoppedSellingGoodsDateSummary.row(answers, waypoints, CheckYourAnswersPage, date),
              StoppedUsingServiceDateSummary.row(answers, waypoints, CheckYourAnswersPage, date),

            ).flatten
          )

          status(result) mustBe OK

          contentAsString(result) mustBe view(waypoints, list, isValid = true, appConfig.iossYourAccountUrl)(request, messages(application)).toString
        }
      }

      "must include StopSellingGoodsSummary row in the summary list when data is present" in {
        val answersWithStopSellingGoods = answers
          .set(StopSellingGoodsPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(answersWithStopSellingGoods), registration = registration)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[CheckYourAnswersView]
          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
          val list = SummaryListViewModel(
            Seq(
              MoveCountrySummary.row(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage),
              StopSellingGoodsSummary.row(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage),
              EuCountrySummary.rowNewCountry(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage),
              MoveDateSummary.rowMoveDate(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage, date),
              EuVatNumberSummary.rowEuVatNumber(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage),
              StoppedSellingGoodsDateSummary.row(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage, date),
              StoppedUsingServiceDateSummary.row(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage, date)
            ).flatten
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(waypoints, list, isValid = true, application.injector.instanceOf[FrontendAppConfig].iossYourAccountUrl)(request, msgs).toString

          val stopSellingGoodsRow = StopSellingGoodsSummary.row(answersWithStopSellingGoods, waypoints, CheckYourAnswersPage)
          stopSellingGoodsRow mustBe defined

          val actualValue = stopSellingGoodsRow.value.value.content.asInstanceOf[Text].value

          val expectedValue = msgs("site.yes")

          actualValue mustBe expectedValue
        }
      }

      "must include StopSellingGoodsSummary row with 'site.no' when StopSellingGoodsPage is false" in {
        val answersWithStopSellingGoodsNo = answers
          .set(StopSellingGoodsPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(answersWithStopSellingGoodsNo), registration = registration)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[CheckYourAnswersView]
          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
          val list = SummaryListViewModel(
            Seq(
              MoveCountrySummary.row(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage),
              StopSellingGoodsSummary.row(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage),
              EuCountrySummary.rowNewCountry(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage),
              MoveDateSummary.rowMoveDate(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage, date),
              EuVatNumberSummary.rowEuVatNumber(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage),
              StoppedSellingGoodsDateSummary.row(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage, date),
              StoppedUsingServiceDateSummary.row(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage, date)
            ).flatten
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(waypoints, list, isValid = true, application.injector.instanceOf[FrontendAppConfig].iossYourAccountUrl)(request, msgs).toString

          val stopSellingGoodsRow = StopSellingGoodsSummary.row(answersWithStopSellingGoodsNo, waypoints, CheckYourAnswersPage)
          stopSellingGoodsRow mustBe defined

          val actualValue = stopSellingGoodsRow.value.value.content.asInstanceOf[Text].value

          val expectedValue = msgs("site.no")

          actualValue mustBe expectedValue
        }
      }

      "must include StoppedSellingGoodsDateSummary row in the summary list when data is present" in {
        val testDate = LocalDate.of(2023, 12, 31)
        val answersWithStoppedSellingGoodsDate = answers
          .set(StoppedSellingGoodsDatePage, testDate).success.value

        val application = applicationBuilder(userAnswers = Some(answersWithStoppedSellingGoodsDate), registration = registration)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[CheckYourAnswersView]
          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
          val dates = application.injector.instanceOf[Dates]
          val list = SummaryListViewModel(
            Seq(
              MoveCountrySummary.row(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage),
              StopSellingGoodsSummary.row(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage),
              EuCountrySummary.rowNewCountry(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage),
              MoveDateSummary.rowMoveDate(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage, dates),
              EuVatNumberSummary.rowEuVatNumber(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage),
              StoppedSellingGoodsDateSummary.row(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage, dates),
              StoppedUsingServiceDateSummary.row(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage, dates)
            ).flatten
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(waypoints, list, isValid = true, application.injector.instanceOf[FrontendAppConfig].iossYourAccountUrl)(request, msgs).toString

          val stoppedSellingGoodsDateRow = StoppedSellingGoodsDateSummary.row(answersWithStoppedSellingGoodsDate, waypoints, CheckYourAnswersPage, dates)
          stoppedSellingGoodsDateRow mustBe defined

          val actualValue = stoppedSellingGoodsDateRow.value.value.content.asInstanceOf[Text].value
          val expectedValue = dates.formatter.format(testDate)

          actualValue mustBe expectedValue
        }
      }

      "must include StoppedUsingServiceDateSummary row in the summary list when data is present" in {
        val testDate = LocalDate.of(2023, 12, 31)
        val answersWithStoppedUsingServiceDate = answers
          .set(StoppedUsingServiceDatePage, testDate).success.value

        val application = applicationBuilder(userAnswers = Some(answersWithStoppedUsingServiceDate), registration = registration)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(application) {
          implicit val msgs: Messages = messages(application)
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value
          val view = application.injector.instanceOf[CheckYourAnswersView]
          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
          val dates = application.injector.instanceOf[Dates]
          val list = SummaryListViewModel(
            Seq(
              MoveCountrySummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage),
              StopSellingGoodsSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage),
              EuCountrySummary.rowNewCountry(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage),
              MoveDateSummary.rowMoveDate(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage, dates),
              EuVatNumberSummary.rowEuVatNumber(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage),
              StoppedSellingGoodsDateSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage, dates),
              StoppedUsingServiceDateSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage, dates)
            ).flatten
          )

          status(result) mustBe OK
          contentAsString(result) mustBe view(waypoints, list, isValid = true, application.injector.instanceOf[FrontendAppConfig].iossYourAccountUrl)(request, msgs).toString

          val stoppedUsingServiceDateRow = StoppedUsingServiceDateSummary.row(answersWithStoppedUsingServiceDate, waypoints, CheckYourAnswersPage, dates)
          stoppedUsingServiceDateRow mustBe defined

          val actualValue = stoppedUsingServiceDateRow.value.value.content.asInstanceOf[Text].value
          val expectedValue = dates.formatter.format(testDate)

          actualValue mustBe expectedValue
        }
      }
    }

    ".onSubmit" - {

      "must redirect to the correct page and audit a success event when the validation passes" in {

        when(mockRegistrationConnector.amend(any())(any())) thenReturn
          Future.successful(Right(()))

        val userAnswers = completeUserAnswers
        val application = applicationBuilder(userAnswers = Some(userAnswers), registration = registration)
          .overrides(
            bind[RegistrationConnector].toInstance(mockRegistrationConnector),
            bind[AuditService].toInstance(mockAuditService)
          ).build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)
          val result = route(application, request).value

          val expectedAuditEvent = ExclusionAuditModel(
            ExclusionAuditType.ExclusionRequestSubmitted,
            userAnswersId,
            "",
            vrn.vrn,
            iossNumber,
            userAnswers.toUserAnswersForAudit,
            registration.registration,
            Some(EtmpExclusionReason.TransferringMSID),
            SubmissionResult.Success
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe ApplicationCompletePage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      "must redirect to the failure page and audit a failure event when the validation passes but amend call failures" in {

        when(mockRegistrationConnector.amend(any())(any())) thenReturn
          Future.successful(Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Error occurred")))

        val userAnswers = completeUserAnswers
        val application = applicationBuilder(userAnswers = Some(userAnswers), registration = registration)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          val expectedAuditEvent = ExclusionAuditModel(
            ExclusionAuditType.ExclusionRequestSubmitted,
            userAnswersId,
            "",
            vrn.vrn,
            iossNumber,
            userAnswers.toUserAnswersForAudit,
            registration.registration,
            Some(EtmpExclusionReason.TransferringMSID),
            SubmissionResult.Failure
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.SubmissionFailureController.onPageLoad().url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      "when the user has not answered all necessary data" - {
        "the user is redirected when the incomplete prompt is shown" - {
          "to the Eu Country page when the EU country is missing" in {
            val answers = completeUserAnswers.remove(EuCountryPage).success.value

            val application = applicationBuilder(userAnswers = Some(answers), registration = registration)
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe controllers.routes.EuCountryController.onPageLoad(waypoints).url
            }
          }

          "to the Move Date page when the move date is missing" in {
            val answers = completeUserAnswers.remove(MoveDatePage).success.value

            val application = applicationBuilder(userAnswers = Some(answers), registration = registration)
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe controllers.routes.MoveDateController.onPageLoad(waypoints).url
            }
          }

          "to the EU VAT Number page when the VAT number is missing" in {
            val answers = completeUserAnswers.remove(EuVatNumberPage).success.value

            val application = applicationBuilder(userAnswers = Some(answers), registration = registration)
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(application) {
              val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt = true).url)
              val result = route(application, request).value

              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe controllers.routes.EuVatNumberController.onPageLoad(waypoints).url
            }
          }
        }
      }
    }
  }
}

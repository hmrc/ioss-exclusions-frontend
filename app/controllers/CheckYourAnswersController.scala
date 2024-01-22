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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import date.Dates
import logging.Logging
import models.CheckMode
import models.etmp.EtmpExclusionReason
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.{EuCountrySummary, MoveDateSummary, TaxNumberSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            dates: Dates,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            registrationService: RegistrationService
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val thisPage = CheckYourAnswersPage
      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))

      val euCountrySummaryRow = EuCountrySummary.rowNewCountry(request.userAnswers, waypoints, thisPage)
      val moveDateSummaryRow = MoveDateSummary.rowMoveDate(request.userAnswers, waypoints, thisPage, dates)
      val taxNumberSummaryRow = TaxNumberSummary.rowTaxNumber(request.userAnswers, waypoints, thisPage)

      val list = SummaryListViewModel(
        rows = Seq(
          euCountrySummaryRow,
          moveDateSummaryRow,
          taxNumberSummaryRow
        ).flatten
      )

      val isValid = validate()
      Ok(view(waypoints, list, isValid))
  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      getFirstValidationErrorRedirect(waypoints) match {
        case Some(errorRedirect) => if (incompletePrompt) {
          errorRedirect.toFuture
        } else {
          Redirect(routes.CheckYourAnswersController.onPageLoad()).toFuture
        }

        case None =>
          registrationService.amendRegistration(
            request.userAnswers,
            Some(EtmpExclusionReason.TransferringMSID),
            request.vrn,
            request.iossNumber,
            request.registrationWrapper
          ).map {
            case Right(_) =>
              Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, request.userAnswers).route)
            case Left(e) =>
              logger.error(s"Failure to submit self exclusion ${e.body}")
              Redirect(routes.SubmissionFailureController.onPageLoad())
          }
      }
  }
}

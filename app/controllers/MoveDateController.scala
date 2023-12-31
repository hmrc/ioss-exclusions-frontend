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

import controllers.actions._
import date.Dates
import forms.MoveDateFormProvider
import pages.{EuCountryPage, MoveDatePage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.MoveDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MoveDateController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    sessionRepository: SessionRepository,
                                    identify: IdentifierAction,
                                    getData: DataRetrievalAction,
                                    requireData: DataRequiredAction,
                                    formProvider: MoveDateFormProvider,
                                    dates: Dates,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: MoveDateView
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val form = formProvider()

      val preparedForm = request.userAnswers.get(MoveDatePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      request.userAnswers.get(EuCountryPage).map { country =>
        Ok(view(
          preparedForm,
          country,
          dates.formatter.format(dates.minMoveDate),
          dates.formatter.format(dates.maxMoveDate),
          dates.dateHint,
          waypoints))
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))

  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val form = formProvider()

      form.bindFromRequest().fold(
        formWithErrors => {
          request.userAnswers.get(EuCountryPage).map { country =>
            BadRequest(view(
              formWithErrors,
              country,
              dates.formatter.format(dates.minMoveDate),
              dates.formatter.format(dates.maxMoveDate),
              dates.dateHint,
              waypoints)).toFuture
          }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture)
        },
        exclusionDate =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(MoveDatePage, exclusionDate))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(MoveDatePage.navigate(waypoints, updatedAnswers, updatedAnswers).url)
      )
  }
}

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
import forms.StoppedUsingServiceDateFormProvider
import pages.{StoppedUsingServiceDatePage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.StoppedUsingServiceDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StoppedUsingServiceDateController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   sessionRepository: SessionRepository,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   getRegistration: GetRegistrationAction,
                                                   formProvider: StoppedUsingServiceDateFormProvider,
                                                   dates: Dates,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: StoppedUsingServiceDateView
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData andThen getRegistration) {
    implicit request =>
      val commencementDate = request.registrationWrapper.registration.schemeDetails.commencementDate
      val form = formProvider(dates.today.date, commencementDate)

      val preparedForm = request.userAnswers.get(StoppedUsingServiceDatePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, dates.dateHint, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData andThen getRegistration).async {
    implicit request =>
      val commencementDate = request.registrationWrapper.registration.schemeDetails.commencementDate
      val form = formProvider.apply(dates.today.date, commencementDate)

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, dates.dateHint, waypoints)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(StoppedUsingServiceDatePage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(StoppedUsingServiceDatePage.navigate(waypoints, updatedAnswers, updatedAnswers).url)
      )
  }
}

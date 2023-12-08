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
import forms.TaxNumberFormProvider
import pages.{EuCountryPage, TaxNumberPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TaxNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxNumberController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: TaxNumberFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: TaxNumberView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(TaxNumberPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      request.userAnswers.get(EuCountryPage).map { country =>
        Ok(view(preparedForm, country, waypoints))
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          request.userAnswers.get(EuCountryPage).map { country =>
            Future.successful(BadRequest(view(formWithErrors, country, waypoints)))
          }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TaxNumberPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(TaxNumberPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}

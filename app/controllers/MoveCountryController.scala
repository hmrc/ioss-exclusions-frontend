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
import forms.MoveCountryFormProvider
import models.UserAnswers
import pages.{MoveCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.MoveCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MoveCountryController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       checkNoExclusion: CheckNoExclusionFilter,
                                       formProvider: MoveCountryFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: MoveCountryView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen checkNoExclusion).async {
    implicit request =>
      val preparedForm = request.userAnswers.getOrElse(UserAnswers(request.userId)).get(MoveCountryPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      Future.successful(Ok(view(preparedForm, waypoints)))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen checkNoExclusion).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value => {
          val originalAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
          for {
            updatedAnswers <- Future.fromTry(originalAnswers.set(MoveCountryPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(MoveCountryPage.navigate(waypoints, originalAnswers, updatedAnswers).route)
        }
      )
  }
}

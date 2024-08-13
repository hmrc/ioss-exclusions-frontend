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

package controllers.reversal

import config.FrontendAppConfig
import controllers.actions._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.CancelLeaveSchemeCompleteView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CancelLeaveSchemeCompleteController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               config: FrontendAppConfig,
                                               view: CancelLeaveSchemeCompleteView,
                                               identify: IdentifierAction,
                                               getData: DataRetrievalAction,
                                               sessionRepository: SessionRepository,
                                               val controllerComponents: MessagesControllerComponents
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      request.userAnswers match {
        case Some(userAnswers) =>
          sessionRepository.clear(userAnswers.id).map { _ =>
            Ok(view(config.iossYourAccountUrl))
          }
        case None =>
          Ok(view(config.iossYourAccountUrl)).toFuture

      }
  }
}

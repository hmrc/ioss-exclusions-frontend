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

import config.FrontendAppConfig
import controllers.actions._
import date.Dates
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ApplicationCompleteView

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class ApplicationCompleteController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               clock: Clock,
                                               dates: Dates,
                                               config: FrontendAppConfig,
                                               view: ApplicationCompleteView,
                                               identify: IdentifierAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               val controllerComponents: MessagesControllerComponents
                                             ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      // TODO: Change to business rules later
      val leaveDate = dates.formatter.format(LocalDate.now(clock).plusDays(1))
      val cancelDate = dates.formatter.format(LocalDate.now(clock))

      Ok(view(config.iossYourAccountUrl, leaveDate, cancelDate))
  }
}

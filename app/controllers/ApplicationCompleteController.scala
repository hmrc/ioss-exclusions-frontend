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
import models.requests.DataRequest
import pages.{EuCountryPage, MoveCountryPage, MoveDatePage, StoppedSellingGoodsDatePage, StoppedUsingServiceDatePage, StopSellingGoodsPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ApplicationCompleteView

import javax.inject.Inject

class ApplicationCompleteController @Inject()(
                                               override val messagesApi: MessagesApi,
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
      request.userAnswers.get(MoveCountryPage).flatMap { isMovingCountry =>
        if (isMovingCountry) {
          onMovingBusiness()
        } else {
          request.userAnswers.get(StopSellingGoodsPage).flatMap { stopSellingGoods =>
            if (stopSellingGoods) {
              onStopSellingGoods()
            } else {
              onStopUsingService()
            }
          }
        }
      }.getOrElse(Redirect(routes.JourneyRecoveryController.onPageLoad()))
  }

  private def onMovingBusiness()(implicit request: DataRequest[AnyContent]): Option[Result] = {
    val messages: Messages = implicitly[Messages]

    for {
      country <- request.userAnswers.get(EuCountryPage)
      leaveDate <- request.userAnswers.get(MoveDatePage)
    } yield {
      val maxChangeDate = leaveDate.plusMonths(1).withDayOfMonth(dates.MoveDayOfMonthSplit)

      Ok(view(
        config.iossYourAccountUrl,
        dates.formatter.format(leaveDate),
        dates.formatter.format(maxChangeDate),
        Some(messages("applicationComplete.moving.text", country.name)),
        Some(messages("applicationComplete.next.info.bullet0", country.name, dates.formatter.format(maxChangeDate)))
      ))
    }
  }


  private def onStopSellingGoods()(implicit request: DataRequest[_]): Option[Result] = {
    val messages: Messages = implicitly[Messages]

    request.userAnswers.get(StoppedSellingGoodsDatePage).map { _ =>
      val leaveDate = dates.getLeaveDateWhenStoppedSellingGoods
      Ok(view(
        config.iossYourAccountUrl,
        dates.formatter.format(leaveDate),
        dates.formatter.format(leaveDate.minusDays(1)),
        Some(messages("applicationComplete.stopSellingGoods.text"))
      ))
    }
  }

  private def onStopUsingService()(implicit request: DataRequest[_]): Option[Result] = {
    request.userAnswers.get(StoppedUsingServiceDatePage).map { stoppedUsingServiceDate =>
      val leaveDate = dates.getLeaveDateWhenStoppedUsingService(stoppedUsingServiceDate)
      Ok(view(
        config.iossYourAccountUrl,
        dates.formatter.format(leaveDate),
        dates.formatter.format(leaveDate.minusDays(1))
      ))
    }
  }
}

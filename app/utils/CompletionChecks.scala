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

package utils

import models._
import models.requests.DataRequest
import pages._
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}

import scala.concurrent.Future

trait CompletionChecks {

  protected def withCompleteDataModel[A](index: Index, data: Index => Option[A], onFailure: Option[A] => Result)
                                        (onSuccess: => Result): Result = {
    val incomplete = data(index)
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteDataAsync[A](data: () => Seq[A], onFailure: Seq[A] => Future[Result])
                                        (onSuccess: => Future[Result]): Future[Result] = {

    val incomplete = data()
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  private def isEuCountryValid()(implicit request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(EuCountryPage).isDefined

  private def isMoveDateValid()(implicit request: DataRequest[AnyContent]): Boolean =
    request.userAnswers.get(MoveDatePage).isDefined


  def validate()(implicit request: DataRequest[AnyContent]): Boolean = {
    isEuCountryValid() && isMoveDateValid()
  }

  def getFirstValidationErrorRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = {
    incompleteEuCountryRedirect(waypoints) orElse
      incompleteMoveDateRedirect(waypoints)
  }

  private def incompleteEuCountryRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = if (!isEuCountryValid()) {
    Some(Redirect(controllers.routes.EuCountryController.onPageLoad(waypoints)))
  } else {
    None
  }

  private def incompleteMoveDateRedirect(waypoints: Waypoints)(implicit request: DataRequest[AnyContent]): Option[Result] = if (!isMoveDateValid()) {
    Some(Redirect(controllers.routes.MoveDateController.onPageLoad(waypoints)))
  } else {
    None
  }
}

/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.actions

import controllers.routes
import logging.Logging
import models.UserAnswers
import models.requests.OptionalDataRequest
import pages.MoveCountryPage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CannotAccessFilter()(implicit val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] with Logging {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    request.userAnswers.getOrElse(UserAnswers(request.userId)).get(MoveCountryPage) match {
      case Some(true) =>
        None.toFuture
      case _ =>
        Some(Redirect(routes.CannotAccessController.onPageLoad().url)).toFuture
    }
  }
}

class CannotAccessFilterProvider @Inject()()(implicit val executionContext: ExecutionContext) {

  def apply(): CannotAccessFilter = new CannotAccessFilter()
}
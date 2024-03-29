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

import logging.Logging
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.{NoLongerSupplies, TransferringMSID, VoluntarilyLeaves}
import models.requests.OptionalDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckExclusionFilterImpl @Inject()(clock: Clock)(implicit val executionContext: ExecutionContext)
  extends CheckExclusionFilter with Logging {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

    maybeExclusion match {
      case Some(exclusion) if Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).contains(exclusion.exclusionReason) &&
        LocalDate.now(clock).isBefore(exclusion.effectiveDate) =>
        Future.successful(None)
      case _ =>
        Future.successful(Some(Redirect(controllers.reversal.routes.CancelLeaveSchemeErrorController.onPageLoad().url)))
    }
  }
}

trait CheckExclusionFilter extends ActionFilter[OptionalDataRequest]

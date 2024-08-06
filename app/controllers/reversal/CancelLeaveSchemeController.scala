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
import forms.CancelLeaveSchemeFormProvider
import logging.Logging
import models.UserAnswers
import models.audit.ExclusionAuditType
import models.etmp.EtmpExclusionReason
import models.requests.OptionalDataRequest
import pages.{CancelLeaveSchemeCompletePage, CancelLeaveSchemePage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CancelLeaveSchemeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CancelLeaveSchemeController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalAction,
                                             checkExclusion: CheckExclusionFilter,
                                             formProvider: CancelLeaveSchemeFormProvider,
                                             config: FrontendAppConfig,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: CancelLeaveSchemeView,
                                             registrationService: RegistrationService,
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen checkExclusion).async {
    implicit request =>

      sessionRepository.clear(request.userId).flatMap { _ =>
        val preparedForm = request.userAnswers.getOrElse(UserAnswers(request.userId)).get(CancelLeaveSchemePage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Future.successful(Ok(view(preparedForm, waypoints)))
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = (identify andThen getData andThen checkExclusion).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        hasCancelled => {
          val originalAnswers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId))
          for {
            updatedAnswers <- Future.fromTry(originalAnswers.set(CancelLeaveSchemePage, hasCancelled))
            _ <- sessionRepository.set(updatedAnswers)
            result <- amendRegistration(waypoints, hasCancelled, updatedAnswers)
          } yield result
        }
      )
  }

  private def amendRegistration(waypoints: Waypoints, hasCancelled: Boolean, updatedAnswers: UserAnswers)
                               (implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    if (hasCancelled) {
      registrationService.amendRegistrationAndAudit(
        request.userId,
        request.vrn,
        request.iossNumber,
        updatedAnswers,
        request.registrationWrapper.registration,
        Some(EtmpExclusionReason.Reversal),
        ExclusionAuditType.ReversalRequestSubmitted
      ).map {
        case Right(_) =>
          Redirect(CancelLeaveSchemeCompletePage.route(waypoints).url)
        case Left(e) =>
          logger.error(s"Failure to submit self exclusion ${e.body}")
          Redirect(routes.CancelLeaveSchemeSubmissionFailureController.onPageLoad())
      }
    } else {
      Future.successful(Redirect(config.iossYourAccountUrl))
    }
  }
}

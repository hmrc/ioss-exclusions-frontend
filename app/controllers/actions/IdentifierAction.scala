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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.routes
import models.requests.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import services.AccountService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject()(
                                               override val authConnector: AuthConnector,
                                               config: FrontendAppConfig,
                                               val parser: BodyParsers.Default,
                                               registrationConnector: RegistrationConnector,
                                               accountService: AccountService
                                             )
                                             (implicit val executionContext: ExecutionContext) extends IdentifierAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId and
      Retrievals.allEnrolments and
      Retrievals.affinityGroup and
      Retrievals.confidenceLevel
    ) {
      case Some(internalId) ~ enrolments ~ Some(Organisation) ~ _ =>
        getRegistrationAndBlock(request, block, internalId, enrolments)

      case Some(internalId) ~ enrolments ~ Some(Individual) ~ confidence =>
        if (confidence >= ConfidenceLevel.L250) {
          getRegistrationAndBlock(request, block, internalId, enrolments)
        } else {
          throw InsufficientConfidenceLevel("Insufficient confidence level")
        }

      case _ =>
        throw new UnauthorizedException("Unable to retrieve authorisation data")
    }
  } recover {
    case _: NoActiveSession =>
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _: AuthorisationException =>
      Redirect(routes.NotRegisteredController.onPageLoad())
  }

  private def getRegistrationAndBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result], internalId: String, enrolments: Enrolments)
                                        (implicit hc: HeaderCarrier): Future[Result] = {
    val maybeVrn = findVrnFromEnrolments(enrolments)
    val futureMaybeIossNumber = findIossFromEnrolments(enrolments)

    futureMaybeIossNumber.flatMap { maybeIossNumber =>

      (maybeVrn, maybeIossNumber) match {
        case (Some(vrn), Some(iossNumber)) =>
          registrationConnector.get()
            .flatMap { registrationWrapper =>
              block(IdentifierRequest(request, internalId, vrn, iossNumber, registrationWrapper))
            }
            .recover {
              case e: Exception => throw new UnauthorizedException(s"No registration found ${e.getMessage}")
            }
        case _ =>
          throw InsufficientEnrolments(s"VAT enrolment was $maybeVrn and IOSS enrolment was $maybeIossNumber")
      }
    }
  }

  private def findVrnFromEnrolments(enrolments: Enrolments): Option[Vrn] =
    enrolments.enrolments.find(_.key == "HMRC-MTD-VAT")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VRN").map(e => Vrn(e.value))
      } orElse enrolments.enrolments.find(_.key == "HMCE-VATDEC-ORG")
      .flatMap {
        enrolment =>
          enrolment.identifiers.find(_.key == "VATRegNo").map(e => Vrn(e.value))
      }

  private def findIossFromEnrolments(enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val filteredIossNumbers = enrolments
      .enrolments
      .filter(_.key == "HMRC-IOSS-ORG")
      .flatMap(_.identifiers.filter(_.key == "IOSSNumber").map(_.value))
      .toSeq

    filteredIossNumbers match {
      case firstEnrolment :: Nil => Some(firstEnrolment).toFuture
      case multipleEnrolments if multipleEnrolments.nonEmpty =>
        accountService.getLatestAccount().map(x => Some(x))
      case _ => None.toFuture
    }
  }

}

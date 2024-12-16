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

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.TestAuthRetrievals._
import controllers.routes
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AccountService, UrlBuilderService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IdentifierActionSpec extends SpecBase with BeforeAndAfterEach {

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockAccountService = mock[AccountService]
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]

  private val vatEnrolmentWithNoIossEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatAndIossEnrolment = Enrolments(Set(
    Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated"),
    Enrolment("HMRC-IOSS-ORG", Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")), "Activated")
  ))

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  private type RetrievalsType = Option[String] ~ Enrolments ~ Option[AffinityGroup] ~ ConfidenceLevel

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
    reset(mockAccountService)
    reset(mockAuthConnector)
  }

  val urlBuilder: Application => UrlBuilderService =
    (application: Application) => application.injector.instanceOf[UrlBuilderService]

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new MissingBearerToken),
            appConfig,
            bodyParsers,
            mockRegistrationConnector,
            mockAccountService,
            urlBuilder(application)
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest(GET, "/example"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new BearerTokenExpired),
            appConfig,
            bodyParsers,
            mockRegistrationConnector,
            mockAccountService,
            urlBuilder(application)
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest(GET, "/example"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientEnrolments),
            appConfig,
            bodyParsers,
            mockRegistrationConnector,
            mockAccountService,
            urlBuilder(application)
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest(GET, "/fake"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()
        val config = application.injector.instanceOf[FrontendAppConfig]

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
            appConfig,
            bodyParsers,
            mockRegistrationConnector,
            mockAccountService,
            urlBuilder(application)
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest(GET, "/fake"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(s"${config.ivUpliftUrl}?origin=IOSS&confidenceLevel=250")
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            appConfig,
            bodyParsers,
            mockRegistrationConnector,
            mockAccountService,
            urlBuilder(application)
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            appConfig,
            bodyParsers,
            mockRegistrationConnector,
            mockAccountService,
            urlBuilder(application)
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.NotRegisteredController.onPageLoad().url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            new FakeFailingAuthConnector(new UnsupportedCredentialRole),
            appConfig,
            bodyParsers,
            mockRegistrationConnector,
            mockAccountService,
            urlBuilder(application)
          )
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.NotRegisteredController.onPageLoad().url)
        }
      }
    }
  }
  "when the user is logged in as an individual without CL >=250" - {

    "must succeed and retrieve an ETMP registration" in {

      val application = applicationBuilder()
        .configure(
          "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
        )
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some("id") ~ vatAndIossEnrolment ~ Some(Individual) ~ ConfidenceLevel.L250))
        when(mockRegistrationConnector.get()(any())) thenReturn registrationWrapper.toFuture

        val action = new AuthenticatedIdentifierAction(
          mockAuthConnector,
          application.injector.instanceOf[FrontendAppConfig],
          bodyParsers,
          mockRegistrationConnector,
          mockAccountService,
          urlBuilder(application)
        )
        val controller = new Harness(action)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustEqual OK
        verify(mockRegistrationConnector, times(1)).get()(any())
      }
    }

    "must return Unauthorized" in {

      val application = applicationBuilder()
        .configure(
          "features.enrolment.ioss-enrolment-key" -> "HMRC-IOSS-ORG"
        )
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      val config = application.injector.instanceOf[FrontendAppConfig]
      
      running(application) {
        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some("id") ~ vatAndIossEnrolment ~ Some(Individual) ~ ConfidenceLevel.L50))
        when(mockRegistrationConnector.get()(any())) thenReturn registrationWrapper.toFuture

        val action = new AuthenticatedIdentifierAction(
          mockAuthConnector,
          application.injector.instanceOf[FrontendAppConfig],
          bodyParsers,
          mockRegistrationConnector,
          mockAccountService,
          urlBuilder(application)
        )
        val controller = new Harness(action)
        val result = controller.onPageLoad()(FakeRequest(GET, "/fake"))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(s"${config.ivUpliftUrl}?origin=IOSS&confidenceLevel=250")
        verifyNoInteractions(mockRegistrationConnector)
      }
    }
  }

  "when the user has logged in as an Organisation Admin with strong credentials but no vat enrolment" - {

    "must be redirected to the Not Registered page" in {
      val application = applicationBuilder(None).build()

      val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

      running(application) {
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some("id") ~ Enrolments(Set.empty) ~ Some(Organisation) ~ ConfidenceLevel.L50))

        val action = new AuthenticatedIdentifierAction(
          mockAuthConnector,
          appConfig,
          bodyParsers,
          mockRegistrationConnector,
          mockAccountService,
          urlBuilder(application)
        )
        val controller = new Harness(action)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
      }
    }
  }

  "when the user has logged in as an Individual with a VAT enrolment and strong credentials, but confidence level less then 250" - {

    "must be redirected to the Not Registered page" in {
      val application = applicationBuilder(None).build()

      val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
      val config = application.injector.instanceOf[FrontendAppConfig]

      running(application) {
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some("id") ~ vatEnrolmentWithNoIossEnrolment ~ Some(Individual) ~ ConfidenceLevel.L50))

        val action = new AuthenticatedIdentifierAction(
          mockAuthConnector,
          appConfig,
          bodyParsers,
          mockRegistrationConnector,
          mockAccountService,
          urlBuilder(application)
        )
        val controller = new Harness(action)
        val result = controller.onPageLoad()(FakeRequest(GET, "/fake"))

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must startWith(s"${config.ivUpliftUrl}?origin=IOSS&confidenceLevel=250")
      }
    }
  }

  "when the user has logged in as an Individual without a VAT enrolment" - {

    "must be redirected to the Not Registered page" in {
      val application = applicationBuilder(None).build()

      val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

      running(application) {
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some("id") ~ Enrolments(Set.empty) ~ Some(Individual) ~ ConfidenceLevel.L200))

        val action = new AuthenticatedIdentifierAction(
          mockAuthConnector,
          appConfig,
          bodyParsers,
          mockRegistrationConnector,
          mockAccountService,
          urlBuilder(application)
        )
        val controller = new Harness(action)
        val result = controller.onPageLoad()(FakeRequest(GET, "/fake"))

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must startWith(s"${appConfig.ivUpliftUrl}?origin=IOSS&confidenceLevel=250")
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}

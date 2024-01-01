package services

import base.SpecBase
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime

class RegistrationServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit private lazy val ar: AuthorisedMandatoryIossRequest[AnyContent] = AuthorisedMandatoryIossRequest(FakeRequest(), userId, vrn, iossNumber)

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  ".amendRegistration" - {

    "must create registration request and return a successful ETMP enrolment response" in {

      val amendRegistrationResponse =
        AmendRegistrationResponse(
          processingDateTime = LocalDateTime.now(stubClock),
          formBundleNumber = "123456789",
          vrn = vrn.vrn,
          iossReference = "test",
          businessPartner = "test businessPartner"
        )

      when(mockRegistrationConnector.amendRegistration(etmpAmendRegistrationRequest)) thenReturn Right(amendRegistrationResponse).toFuture

      val app = applicationBuilder
        .build()

      running(app) {

        registrationService.amendRegistration(etmpAmendRegistrationRequest).futureValue mustBe AmendSucceeded
        verify(mockRegistrationConnector, times(1)).amendRegistration(eqTo(etmpAmendRegistrationRequest))
      }
    }
  }

}

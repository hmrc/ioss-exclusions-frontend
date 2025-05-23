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

package generators

import models.*
import models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}
import models.etmp.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Gen.option
import uk.gov.hmrc.domain.Vrn

import java.time.{LocalDate, LocalDateTime}
import java.time.temporal.ChronoUnit

trait ModelGenerators {

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      Gen.oneOf(Country.euCountries)
    }

  implicit val arbitraryRegistrationWrapper: Arbitrary[RegistrationWrapper] = Arbitrary {
    for {
      vatInfo <- arbitrary[VatCustomerInfo]
      registration <- arbitrary[EtmpDisplayRegistration]
    } yield RegistrationWrapper(vatInfo, registration)
  }

  implicit lazy val arbitraryDesAddress: Arbitrary[DesAddress] =
    Arbitrary {
      for {
        line1 <- arbitrary[String]
        line2 <- Gen.option(arbitrary[String])
        line3 <- Gen.option(arbitrary[String])
        line4 <- Gen.option(arbitrary[String])
        line5 <- Gen.option(arbitrary[String])
        postCode <- Gen.option(arbitrary[String])
        countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
      } yield DesAddress(line1, line2, line3, line4, line5, postCode, countryCode)
    }

  implicit val arbitraryVatInfo: Arbitrary[VatCustomerInfo] = Arbitrary {
    for {
      address <- arbitrary[DesAddress]
      registrationDate <- arbitrary[LocalDate]
      partOfVatGroup <- arbitrary[Boolean]
      organisationName <- Gen.option(arbitrary[String])
      individualName <- arbitrary[String]
      singleMarketIndicator <- arbitrary[Boolean]
      deregistrationDecisionDate <- Gen.option(arbitrary[LocalDate])
      overseasIndicator <- arbitrary[Boolean]

    } yield VatCustomerInfo(
      address,
      Some(registrationDate),
      partOfVatGroup,
      organisationName,
      if (organisationName.isEmpty) {
        Some(individualName)
      } else {
        None
      },
      singleMarketIndicator,
      deregistrationDecisionDate,
      overseasIndicator
    )
  }

  implicit lazy val arbitraryVatNumberTraderId: Arbitrary[VatNumberTraderId] =
    Arbitrary {
      for {
        vatNumber <- Gen.alphaNumStr
      } yield VatNumberTraderId(vatNumber)
    }

  implicit val arbitraryDisplayEtmpEuRegistrationDetails: Arbitrary[EtmpDisplayEuRegistrationDetails] = {
    Arbitrary {
      for {
        countryOfRegistration <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString.toUpperCase)
        traderId <- arbitrary[String]
        tradingName <- arbitrary[String]
        fixedEstablishmentAddressLine1 <- arbitrary[String]
        fixedEstablishmentAddressLine2 <- Gen.option(arbitrary[String])
        townOrCity <- arbitrary[String]
        regionOrState <- Gen.option(arbitrary[String])
        postcode <- Gen.option(arbitrary[String])
      } yield {
        EtmpDisplayEuRegistrationDetails(
          countryOfRegistration,
          Some(traderId),
          None,
          tradingName,
          fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2,
          townOrCity,
          regionOrState,
          postcode
        )
      }
    }
  }

  implicit val arbitraryEtmpEuRegistrationDetails: Arbitrary[EtmpEuRegistrationDetails] = {
    Arbitrary {
      for {
        countryOfRegistration <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString.toUpperCase)
        traderId <- arbitrary[VatNumberTraderId]
        tradingName <- arbitrary[String]
        fixedEstablishmentAddressLine1 <- arbitrary[String]
        fixedEstablishmentAddressLine2 <- Gen.option(arbitrary[String])
        townOrCity <- arbitrary[String]
        regionOrState <- Gen.option(arbitrary[String])
        postcode <- Gen.option(arbitrary[String])
      } yield {
        EtmpEuRegistrationDetails(
          countryOfRegistration = countryOfRegistration,
          traderId = traderId,
          tradingName = tradingName,
          fixedEstablishmentAddressLine1 = fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = fixedEstablishmentAddressLine2,
          townOrCity = townOrCity,
          regionOrState = regionOrState,
          postcode = postcode
        )
      }
    }
  }

  implicit lazy val arbitraryWebsite: Arbitrary[EtmpWebsite] =
    Arbitrary {
      for {
        websiteAddress <- Gen.alphaStr
      } yield EtmpWebsite(websiteAddress)
    }


  implicit val arbitraryEtmpPreviousEURegistrationDetails: Arbitrary[EtmpPreviousEuRegistrationDetails] = {
    Arbitrary {
      for {
        issuedBy <- arbitrary[String]
        registrationNumber <- arbitrary[String]
        schemeType <- Gen.oneOf(SchemeType.values)
        intermediaryNumber <- Gen.option(arbitrary[String])
      } yield EtmpPreviousEuRegistrationDetails(issuedBy, registrationNumber, schemeType, intermediaryNumber)
    }
  }

  implicit val arbitraryEtmpSchemeDetails: Arbitrary[EtmpSchemeDetails] = {
    Arbitrary {
      for {
        commencementDate <- arbitrary[LocalDate]
        euRegistrationDetails <- Gen.listOfN(5, arbitraryEtmpEuRegistrationDetails.arbitrary)
        previousEURegistrationDetails <- Gen.listOfN(5, arbitraryEtmpPreviousEURegistrationDetails.arbitrary)
        websites <- Gen.listOfN(10, arbitraryWebsite.arbitrary)
        contactName <- arbitrary[String]
        businessTelephoneNumber <- arbitrary[String]
        businessEmailId <- arbitrary[String]
        nonCompliantReturns <- Gen.option(arbitrary[Int].toString)
        nonCompliantPayments <- Gen.option(arbitrary[Int].toString)
      } yield
        EtmpSchemeDetails(
          commencementDate,
          euRegistrationDetails,
          previousEURegistrationDetails,
          websites,
          contactName,
          businessTelephoneNumber,
          businessEmailId,
          nonCompliantReturns,
          nonCompliantPayments
        )
    }
  }


  implicit val arbitraryDisplayEtmpSchemeDetails: Arbitrary[EtmpDisplaySchemeDetails] = {
    Arbitrary {
      for {
        commencementDate <- arbitrary[LocalDate]
        euRegistrationDetails <- Gen.listOfN(5, arbitraryDisplayEtmpEuRegistrationDetails.arbitrary)
        previousEURegistrationDetails <- Gen.listOfN(5, arbitraryEtmpPreviousEURegistrationDetails.arbitrary)
        websites <- Gen.listOfN(10, arbitraryWebsite.arbitrary)
        contactName <- arbitrary[String]
        businessTelephoneNumber <- arbitrary[String]
        businessEmailId <- arbitrary[String]
        nonCompliantReturns <- Gen.option(arbitrary[Int].toString)
        nonCompliantPayments <- Gen.option(arbitrary[Int].toString)
      } yield
        EtmpDisplaySchemeDetails(
          commencementDate.toString,
          euRegistrationDetails,
          previousEURegistrationDetails,
          websites,
          contactName,
          businessTelephoneNumber,
          businessEmailId,
          unusableStatus = false,
          nonCompliantReturns,
          nonCompliantPayments
        )
    }
  }


  implicit lazy val arbitraryEtmpTradingName: Arbitrary[EtmpTradingName] =
    Arbitrary {
      for {
        tradingName <- Gen.alphaStr
      } yield EtmpTradingName(tradingName)
    }

  implicit lazy val arbitraryEtmpExclusion: Arbitrary[EtmpExclusion] = {
    Arbitrary {
      for {
        exclusionReason <- Gen.oneOf[EtmpExclusionReason](EtmpExclusionReason.values)
        effectiveDate <- arbitrary[Int].map(n => LocalDate.ofEpochDay(n))
        decisionDate <- arbitrary[Int].map(n => LocalDate.ofEpochDay(n))
        quarantine <- arbitrary[Boolean]
      } yield EtmpExclusion(
        exclusionReason,
        effectiveDate,
        decisionDate,
        quarantine
      )
    }
  }

  implicit lazy val arbitraryEtmpExclusionReason: Arbitrary[EtmpExclusionReason] =
    Arbitrary {
      Gen.oneOf(EtmpExclusionReason.values)
    }


  implicit lazy val arbitraryAdminUse: Arbitrary[EtmpAdminUse] =
    Arbitrary {
      for {
        changeDate <- arbitrary[LocalDateTime]
      } yield EtmpAdminUse(Some(changeDate))
    }

  implicit lazy val arbitraryBic: Arbitrary[Bic] = {
    val asciiCodeForA = 65
    val asciiCodeForN = 78
    val asciiCodeForP = 80
    val asciiCodeForZ = 90

    Arbitrary {
      for {
        firstChars <- Gen.listOfN(6, Gen.alphaUpperChar).map(_.mkString)
        char7 <- Gen.oneOf(Gen.alphaUpperChar, Gen.choose(2, 9))
        char8 <- Gen.oneOf(
          Gen.choose(asciiCodeForA, asciiCodeForN).map(_.toChar),
          Gen.choose(asciiCodeForP, asciiCodeForZ).map(_.toChar),
          Gen.choose(0, 9)
        )
        lastChars <- Gen.option(Gen.listOfN(3, Gen.oneOf(Gen.alphaUpperChar, Gen.numChar)).map(_.mkString))
      } yield Bic(s"$firstChars$char7$char8${lastChars.getOrElse("")}").get
    }
  }

  implicit lazy val arbitraryIban: Arbitrary[Iban] =
    Arbitrary {
      Gen.oneOf(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      ).map(v => Iban(v).toOption.get)
    }

  implicit lazy val arbitraryEtmpBankDetails: Arbitrary[EtmpBankDetails] =
    Arbitrary {
      for {
        accountName <- arbitrary[String]
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield EtmpBankDetails(accountName, bic, iban)
    }


  implicit val arbitraryEtmpDisplayRegistration: Arbitrary[EtmpDisplayRegistration] = Arbitrary {
    for {
      etmpTradingNames <- Gen.listOfN(2, arbitraryEtmpTradingName.arbitrary)
      schemeDetails <- arbitrary[EtmpDisplaySchemeDetails]
      bankDetails <- arbitrary[EtmpBankDetails]
      exclusions <- Gen.listOfN(1, arbitraryEtmpExclusion.arbitrary)
      adminUse <- arbitrary[EtmpAdminUse]
    } yield EtmpDisplayRegistration(
      etmpTradingNames,
      schemeDetails,
      bankDetails,
      exclusions,
      adminUse
    )
  }

  implicit lazy val arbitraryVrn: Arbitrary[Vrn] =
    Arbitrary {
      for {
        chars <- Gen.listOfN(9, Gen.numChar)
      } yield Vrn(chars.mkString(""))
    }

  implicit lazy val arbitraryEtmpAdministration: Arbitrary[EtmpAdministration] =
    Arbitrary {
      for {
        messageType <- Gen.oneOf(EtmpMessageType.values)
      } yield EtmpAdministration(messageType, "IOSS")
    }

  implicit lazy val arbitraryEtmpCustomerIdentification: Arbitrary[EtmpAmendCustomerIdentification] =
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
      } yield EtmpAmendCustomerIdentification(s"IM9$vrn")
    }

  implicit lazy val arbitrarySchemeType: Arbitrary[SchemeType] =
    Arbitrary {
      Gen.oneOf(SchemeType.values)
    }

  implicit lazy val arbitraryNonCompliantDetails: Arbitrary[NonCompliantDetails] =
    Arbitrary {
      for {
        nonCompliantReturns <- option(Gen.chooseNum(1, 2))
        nonCompliantPayments <- option(Gen.chooseNum(1, 2))
      } yield {
        NonCompliantDetails(
          nonCompliantReturns = nonCompliantReturns,
          nonCompliantPayments = nonCompliantPayments
        )
      }
    }

  implicit val arbitraryEACDIdentifiers: Arbitrary[EACDIdentifiers] = {
    Arbitrary {
      for {
        key <- Gen.alphaStr
        value <- Gen.alphaStr
      } yield EACDIdentifiers(
        key = key,
        value = value
      )
    }
  }

  implicit val arbitraryEACDEnrolment: Arbitrary[EACDEnrolment] = {
    Arbitrary {
      for {
        service <- Gen.alphaStr
        state <- Gen.alphaStr
        identifiers <- Gen.listOfN(2, arbitraryEACDIdentifiers.arbitrary)
      } yield EACDEnrolment(
        service = service,
        state = state,
        activationDate = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)),
        identifiers = identifiers
      )
    }
  }

  implicit val arbitraryEACDEnrolments: Arbitrary[EACDEnrolments] = {
    Arbitrary {
      for {
        enrolments <- Gen.listOfN(2, arbitraryEACDEnrolment.arbitrary)
      } yield EACDEnrolments(
        enrolments = enrolments
      )
    }
  }

  implicit lazy val arbitraryEtmpAmendRegistrationChangeLog: Arbitrary[EtmpAmendRegistrationChangeLog] =
    Arbitrary {
      for {
        tradingNames <- arbitrary[Boolean]
        fixedEstablishments <- arbitrary[Boolean]
        contactDetails <- arbitrary[Boolean]
        bankDetails <- arbitrary[Boolean]
        reRegistration <- arbitrary[Boolean]
      } yield EtmpAmendRegistrationChangeLog(tradingNames, fixedEstablishments, contactDetails, bankDetails, reRegistration)
    }
}

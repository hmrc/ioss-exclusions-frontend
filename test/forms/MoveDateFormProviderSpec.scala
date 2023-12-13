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

package forms

import date.Dates
import forms.behaviours.DateBehaviours
import org.scalacheck.Gen
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class MoveDateFormProviderSpec extends DateBehaviours {

  implicit val messages: Messages = stubMessages()
  val form = new MoveDateFormProvider(Dates.clock)()

  ".value" - {

    val minDate: LocalDate = LocalDate.now(Dates.clock)

    val validData: Gen[LocalDate] = datesBetween(
      min = minDate,
      max = minDate.plusMonths(1).withDayOfMonth(10)
    )

    behave like dateField(form, "value", validData)

    behave like mandatoryDateField(form, "value", "moveDate.error.required.all")
  }
}

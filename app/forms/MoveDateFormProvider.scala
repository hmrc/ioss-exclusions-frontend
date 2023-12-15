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

import date.{Dates, LocalDateOps}
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class MoveDateFormProvider @Inject()(clock: Clock) extends Mappings {

  private val dates: Dates = new Dates(clock)

  def apply(today: LocalDate = LocalDate.now(clock))(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "moveDate.error.invalid",
        allRequiredKey = "moveDate.error.required.all",
        twoRequiredKey = "moveDate.error.required.two",
        requiredKey    = "moveDate.error.required"
      ).verifying(validDate(today))
    )

  private def validDate(today: LocalDate): Constraint[LocalDate] = {
    val dayOfMonthSplit = 10
    val minDate: LocalDate = (if (today.getDayOfMonth <= dayOfMonthSplit) today.minusMonths(1) else today).withDayOfMonth(1)
    val maxDate: LocalDate = today.plusMonths(1).withDayOfMonth(dayOfMonthSplit)

    Constraint {
      case date if date < minDate => Invalid("moveDate.error.invalid.minDate", dates.digitsFormatter.format(minDate))
      case date if date > maxDate => Invalid("moveDate.error.invalid.maxDate", dates.digitsFormatter.format(maxDate))
      case _ => Valid
    }
  }
}

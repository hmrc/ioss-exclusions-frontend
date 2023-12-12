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

import date.LocalDateOps
import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class MoveDateFormProvider @Inject()(clock: Clock) extends Mappings {

  def apply()(implicit messages: Messages): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "moveDate.error.invalid",
        allRequiredKey = "moveDate.error.required.all",
        twoRequiredKey = "moveDate.error.required.two",
        requiredKey    = "moveDate.error.required"
      ).verifying(validDate("moveDate.error.invalid"))
    )

  private def validDate(errorKey: String): Constraint[LocalDate] = {
    val dayOfMonthSplit = 10
    val now = LocalDate.now(clock)
    val minDate: LocalDate = (if (now.getDayOfMonth <= dayOfMonthSplit) now.minusMonths(1) else now).withDayOfMonth(1)
    val maxDate: LocalDate = now.plusMonths(1).withDayOfMonth(dayOfMonthSplit)

    Constraint {
      case date if minDate <= date && date <= maxDate => Valid
      case _ => Invalid(errorKey)
    }
  }
}

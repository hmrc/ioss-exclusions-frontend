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

import forms.mappings.Mappings
import play.api.data.Form

import java.time.{LocalDate, YearMonth}
import javax.inject.Inject

class StoppedSellingGoodsDateFormProvider @Inject() extends Mappings {

  def apply(currentDate: LocalDate, registrationDate: LocalDate): Form[LocalDate] = {
    val lastDayOfMonth = YearMonth.of(currentDate.getYear, currentDate.getMonthValue)
      .atEndOfMonth()
      .getDayOfMonth

    val endOfPeriod = currentDate.withDayOfMonth(lastDayOfMonth)
    Form(
      "value" -> localDate(
        invalidKey = "stoppedSellingGoodsDate.error.invalid",
        allRequiredKey = "stoppedSellingGoodsDate.error.required.all",
        twoRequiredKey = "stoppedSellingGoodsDate.error.required.two",
        requiredKey = "stoppedSellingGoodsDate.error.required"
      ).verifying(inDateRange(registrationDate, endOfPeriod, "stoppedSellingGoodsDate.error.invalidDateRage"))
    )
  }
}

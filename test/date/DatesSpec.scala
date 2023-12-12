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

package date

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDate

class DatesSpec extends SpecBase with MockitoSugar {
  "LocalDateOps" - {
    "must implement Ordered to be able to use comparison operators: <, >, <=, >=" in {
      val DaysToAdd = 10
      val before = LocalDate.now
      val after = before.plusDays(DaysToAdd)

      before.compare(after) must be < 0
      after.compare(before) must be > 0
      before.compare(before) mustBe 0

      before <= before mustBe true
      before >= before mustBe true

      before < after mustBe true
      after < before mustBe false

      after > before mustBe true
      before > after mustBe false
    }
  }

}

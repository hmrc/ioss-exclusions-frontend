package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class EuCountryFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "euCountry.error.required"
  val invalidKey = "error.boolean"

  val form = new EuCountryFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}

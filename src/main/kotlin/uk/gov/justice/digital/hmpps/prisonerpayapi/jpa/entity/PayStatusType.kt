package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import uk.gov.justice.digital.hmpps.prisonerpayapi.common.PaymentType

enum class PayStatusType(val paymentType: PaymentType) {
  LONG_TERM_SICK(PaymentType.LONG_TERM_SICK),
}

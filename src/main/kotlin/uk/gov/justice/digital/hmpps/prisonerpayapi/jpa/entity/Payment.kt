package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.PaymentType
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
class Payment(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: UUID? = null,

  val prisonCode: String,

  val prisonerNumber: String,

  val eventDate: LocalDate,

  @Enumerated(EnumType.STRING)
  val timeSlot: TimeSlot,

  @Enumerated(EnumType.STRING)
  val paymentType: PaymentType,

  val paymentDateTime: LocalDateTime,

  val paymentAmount: Int,

  var reference: String? = null,
) {

  override fun toString(): String = "Payment(id=$id, prisonCode='$prisonCode', prisonerNumber='$prisonerNumber', eventDate=$eventDate, eventPeriod='$timeSlot', paymentType=$paymentType, paymentDateTime=$paymentDateTime, paymentAmount=$paymentAmount, reference='$reference')"
}

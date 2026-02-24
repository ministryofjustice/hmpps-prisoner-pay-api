package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
class PayRate(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: UUID? = null,

  val prisonCode: String,

  @Enumerated(EnumType.STRING)
  val type: PayStatusType,

  val startDate: LocalDate,

  var rate: Int,

  val createdDateTime: LocalDateTime,

  val createdBy: String,

  var updatedDateTime: LocalDateTime? = null,

  var updatedBy: String? = null,
)

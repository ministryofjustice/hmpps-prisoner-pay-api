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
class PayStatusPeriod(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: UUID? = null,

  val prisonerNumber: String,

  @Enumerated(EnumType.STRING)
  val type: PayStatusType,

  val startDate: LocalDate,

  val endDate: LocalDate?,

  val createdDateTime: LocalDateTime,

  val createdBy: String,
)

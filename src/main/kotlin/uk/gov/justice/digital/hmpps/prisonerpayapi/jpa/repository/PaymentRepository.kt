package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.Payment
import java.time.LocalDate
import java.util.*

@Repository
interface PaymentRepository : JpaRepository<Payment, UUID> {
  @Query(
    """
      select p from Payment p 
      where p.prisonerNumber = :prisonerNumber
      and p.eventDate = :eventDate
    """,
  )
  fun findPayments(prisonerNumber: String, eventDate: LocalDate): List<Payment>
}

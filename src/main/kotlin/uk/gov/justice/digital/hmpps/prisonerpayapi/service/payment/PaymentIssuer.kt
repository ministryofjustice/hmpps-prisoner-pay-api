package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment

import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.Payment
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PaymentRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.util.generateReference

@Component
class PaymentIssuer(
  private val paymentRepository: PaymentRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(
    retryFor = [ConstraintViolationException::class],
    maxAttempts = 3,
    backoff = Backoff(delay = 2, multiplier = 2.0),
  )
  fun issuePayment(payment: Payment): Payment {
    payment.reference = generateReference()

    log.info("Issuing payment: $payment")

    return paymentRepository.save(payment)
  }
}

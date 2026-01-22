package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payment
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PaymentRepository

class PaymentIssuerTest {
  val paymentRepository: PaymentRepository = mock()

  val paymentIssuer = PaymentIssuer(paymentRepository)

  @Test
  fun `should issue payment`() {
    val payment = payment()

    whenever(paymentRepository.save(payment)).thenReturn(payment)

    paymentIssuer.issuePayment(payment)

    verify(paymentRepository).save(payment)
  }
}

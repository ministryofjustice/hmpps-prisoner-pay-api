package uk.gov.justice.digital.hmpps.prisonerpayapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WebClientConfigurationTest {

  @Test
  fun `should convert java non proxy hosts syntax to reactor regex`() {
    val result = toReactorNonProxyHostsPattern("localhost|127.*|envoy-https-proxy|*.svc|*.cluster.local")

    assertThat(result).isEqualTo("^localhost$|^127\\..*$|^envoy-https-proxy$|^.*\\.svc$|^.*\\.cluster\\.local$")
  }

  @Test
  fun `should ignore blank non proxy host entries`() {
    val result = toReactorNonProxyHostsPattern(" localhost | | *.svc ")

    assertThat(result).isEqualTo("^localhost$|^.*\\.svc$")
  }

  @Test
  fun `should return null when non proxy hosts is blank`() {
    assertThat(toReactorNonProxyHostsPattern(" ")).isNull()
    assertThat(toReactorNonProxyHostsPattern(null)).isNull()
  }
}
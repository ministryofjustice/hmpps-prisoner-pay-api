package uk.gov.justice.digital.hmpps.prisonerpayapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.webclient.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

private val regexMetaCharacters = setOf('\\', '.', '^', '$', '+', '?', '(', ')', '[', ']', '{', '}')

@Configuration
class WebClientConfiguration(
  @param:Value($$"${api.base.url.hmpps-auth}") val hmppsAuthBaseUri: String,
  @param:Value($$"${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value($$"${api.timeout:20s}") val timeout: Duration,
) {
  @Bean
  fun proxyWebClientCustomizer(): WebClientCustomizer = WebClientCustomizer { builder ->
    proxyConnector()?.let { builder.clientConnector(it) }
  }

  private fun proxyConnector(): ReactorClientHttpConnector? {
    val proxyHost = System.getProperty("https.proxyHost") ?: System.getProperty("http.proxyHost") ?: return null
    val proxyPort = listOf(System.getProperty("https.proxyPort"), System.getProperty("http.proxyPort"))
      .firstNotNullOfOrNull { it?.toIntOrNull() } ?: 3128
    val nonProxyHosts = toReactorNonProxyHostsPattern(System.getProperty("https.nonProxyHosts") ?: System.getProperty("http.nonProxyHosts"))

    val httpClient = HttpClient.create().proxy { proxy ->
      val builder = proxy
        .type(ProxyProvider.Proxy.HTTP)
        .host(proxyHost)
        .port(proxyPort)

      if (nonProxyHosts != null) {
        builder.nonProxyHosts(nonProxyHosts)
      }
    }

    return ReactorClientHttpConnector(httpClient)
  }

  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)
}

internal fun toReactorNonProxyHostsPattern(nonProxyHosts: String?): String? {
  if (nonProxyHosts.isNullOrBlank()) return null

  val patterns = nonProxyHosts.split('|')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { "^${it.toReactorRegexFragment()}$" }

  return patterns.takeIf { it.isNotEmpty() }?.joinToString("|")
}

private fun String.toReactorRegexFragment(): String = buildString {
  this@toReactorRegexFragment.forEach { char ->
    when {
      char == '*' -> append(".*")
      char in regexMetaCharacters -> append('\\').append(char)
      else -> append(char)
    }
  }
}

package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.prisonerpayapi.integration.config.LocalStackContainer
import uk.gov.justice.digital.hmpps.prisonerpayapi.integration.config.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.prisonerpayapi.integration.config.PostgresContainer
import uk.gov.justice.digital.hmpps.prisonerpayapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.prisonerpayapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal const val USERNAME = "TestUser"
internal const val prisonCode = "RSI"

@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@ExtendWith(HmppsAuthApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Sql("classpath:sql/tear-down-all-data.sql")
abstract class IntegrationTestBase {
  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @MockitoBean
  protected lateinit var clock: Clock

  companion object {
    internal val db = PostgresContainer.instance
    internal val localstack = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      db?.run {
        registry.add("spring.datasource.url", db::getJdbcUrl)
        registry.add("spring.datasource.username", db::getUsername)
        registry.add("spring.datasource.password", db::getPassword)
      }
      localstack?.also { setLocalStackProperties(it, registry) }
    }
  }

  @BeforeEach
  fun setup() {
    whenever(clock.instant()).thenReturn(Instant.now())
    whenever(clock.zone).thenReturn(ZoneId.of("Europe/London"))
  }

  internal fun setAuthorisation(
    username: String? = USERNAME,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(
    clientId = "hmpps-prisoner-pay-api",
    roles = roles,
    username = username,
    scope = scopes,
  )

  internal fun noAuthorisation(
    username: String? = USERNAME,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = {
    println("No auth header set")
  }

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

  internal final inline fun <reified T : Any> WebTestClient.ResponseSpec.success(status: HttpStatus = HttpStatus.OK): T = expectStatus().isEqualTo(status)
    .expectBody(T::class.java)
    .returnResult().responseBody!!

  internal final inline fun <reified T : Any> WebTestClient.ResponseSpec.successList(status: HttpStatus = HttpStatus.OK): List<T> = expectStatus().isEqualTo(status)
    .expectBodyList(T::class.java)
    .returnResult().responseBody!!

  internal final fun WebTestClient.ResponseSpec.badRequest() = expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
    .expectBody(ErrorResponse::class.java)
    .returnResult().responseBody!!

  internal final fun WebTestClient.ResponseSpec.fail(status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR) = expectStatus().isEqualTo(status)
}

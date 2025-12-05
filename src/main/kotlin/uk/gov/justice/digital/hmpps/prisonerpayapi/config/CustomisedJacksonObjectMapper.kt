package uk.gov.justice.digital.hmpps.prisonerpayapi.config

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Configuration
class CustomisedJacksonObjectMapper {
  @Bean
  fun serialiser() = Jackson2ObjectMapperBuilderCustomizer {
    val zoneId = ZoneId.of("Europe/London")
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId)
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(zoneId)

    it.serializers(
      LocalDateSerializer(dateFormatter),
      LocalDateTimeSerializer(dateTimeFormatter),
    )
  }
}

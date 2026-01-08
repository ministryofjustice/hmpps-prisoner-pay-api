package uk.gov.justice.digital.hmpps.prisonerpayapi.config

import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("Europe/London"))

@JacksonComponent
class CustomDateTimeSerializer {

  class DateTimeSerializer : ValueSerializer<LocalDateTime>() {

    override fun serialize(value: LocalDateTime, jgen: JsonGenerator, serializers: SerializationContext) {
      jgen.writeString(value.format(DATETIME_FORMATTER))
    }
  }

  class DateTimeDeserializer : ValueDeserializer<LocalDateTime>() {
    override fun deserialize(jsonParser: JsonParser, ctx: DeserializationContext): LocalDateTime = LocalDateTime.parse(ctx.readValue(jsonParser, String::class.java), DATETIME_FORMATTER)
  }
}

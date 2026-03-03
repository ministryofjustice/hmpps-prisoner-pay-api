package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

inline fun <reified T : Any> JpaRepository<T, UUID>.findOrThrowNotFound(id: UUID): T = this.findById(id).orElseThrow { EntityNotFoundException("${T::class.java.simpleName.spaceOut()} $id not found") }

fun String.spaceOut() = "[A-Z]".toRegex().replace(this) { " ${it.value}" }.trim()

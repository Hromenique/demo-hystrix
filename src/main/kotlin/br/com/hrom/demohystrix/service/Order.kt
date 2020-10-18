package br.com.hrom.demohystrix.service

import java.time.LocalDateTime
import java.util.*

data class Order(
        val id: UUID = UUID.randomUUID(),
        val products: List<String>,
        val createdAt: LocalDateTime = LocalDateTime.now()
)
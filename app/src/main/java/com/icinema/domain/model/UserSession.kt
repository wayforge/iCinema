package com.icinema.domain.model

data class UserSession(
    val username: String,
    val token: String,
    val loginAt: Long
)

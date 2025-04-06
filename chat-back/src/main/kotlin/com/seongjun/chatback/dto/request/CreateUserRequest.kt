package com.seongjun.chatback.dto.request

data class CreateUserRequest(
    val username: String,
    val email: String,
    val password: String
)
package com.seongjun.chatback.dto.request

data class CreateMessageRequest(
    val roomId: Long,
    val content: String,
    val type: String = "TEXT"
)
package com.seongjun.chatback.dto.request

data class CreateRoomRequest(
    val name: String,
    val description: String? = null,
    val isPrivate: Boolean = false,
    val maxUsers: Int? = null,
    val userId:Long = 0
)

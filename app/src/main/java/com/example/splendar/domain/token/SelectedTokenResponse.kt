package com.example.splendar.domain.token

import kotlinx.serialization.Serializable

@Serializable
data class SelectedTokenResponse(
    val status: String,
    val data: SelectedToken?,
    val message: String? = null
)

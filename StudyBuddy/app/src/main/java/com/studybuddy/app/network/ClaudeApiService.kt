package com.studybuddy.app.network

import com.studybuddy.app.network.models.ClaudeRequest
import com.studybuddy.app.network.models.ClaudeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ClaudeApiService {
    @POST("messages")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("content-type") contentType: String = "application/json",
        @Body request: ClaudeRequest
    ): Response<ClaudeResponse>
}

package com.studybuddy.app.network

import com.studybuddy.app.network.models.GeminiRequest
import com.studybuddy.app.network.models.GeminiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApiService {

    /**
     * Generates content for a given model.
     *
     * Endpoint: POST /models/{model}:generateContent?key={apiKey}
     *
     * Common models:
     *   gemini-2.0-flash          — fast, cheap, great for extraction + general chat
     *   gemini-2.5-pro-preview-05-06 — highest reasoning capability
     */
    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

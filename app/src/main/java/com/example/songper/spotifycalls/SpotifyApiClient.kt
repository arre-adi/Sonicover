package com.example.sonicover.spotifycalls

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SpotifyApiClient {
    private const val BASE_URL = "https://api.spotify.com/v1/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SpotifyApiService = retrofit.create(SpotifyApiService::class.java)
}
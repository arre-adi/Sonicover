package com.example.songper.spotifycalls

import CurrentlyPlaying
import UserProfile
import retrofit2.http.GET
import retrofit2.http.Header

interface SpotifyApiService {
    @GET("me")
    suspend fun getUserProfile(
        @Header("Authorization") accessToken: String
    ): UserProfile

    @GET("me/player/currently-playing")
    suspend fun getCurrentlyPlaying(
        @Header("Authorization") accessToken: String
    ): CurrentlyPlaying
}
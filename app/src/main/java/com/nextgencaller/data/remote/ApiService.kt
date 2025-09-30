package com.nextgencaller.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("users/lookup")
    suspend fun findUserByPhoneNumber(@Query("phone") phoneNumber: String): UserLookupResponse
}

data class UserLookupResponse(
    val found: Boolean,
    val userId: String?,
    val name: String?,
    val photoUrl: String?
)
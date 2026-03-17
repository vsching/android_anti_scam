/**
 * Retrofit interface for the Safe Anot? backend API.
 */
package com.safeanot.app.data.remote

import com.safeanot.app.data.remote.model.CheckRequest
import com.safeanot.app.data.remote.model.CheckResponse
import com.safeanot.app.data.remote.model.LatestMetadataResponse
import okhttp3.ResponseBody
import com.safeanot.app.data.remote.model.AlertDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SafeAnotApi {

    @GET("api/data/latest")
    suspend fun getLatestMetadata(): LatestMetadataResponse

    @Streaming
    @GET("api/data/full")
    suspend fun getFullDatabase(): ResponseBody

    @Streaming
    @GET("api/data/delta")
    suspend fun getDelta(@Query("since") since: String): ResponseBody

    @Streaming
    @GET("api/data/bloom")
    suspend fun getBloomFilter(): ResponseBody

    @POST("api/check")
    suspend fun checkDomain(@Body request: CheckRequest): CheckResponse

    @GET("api/alerts")
    suspend fun getAlerts(@Query("region") region: String? = null): List<AlertDto>
}

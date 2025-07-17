package com.sssk.myhealthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.ZoneId

/**
 * Data class representing a single step interval.
 */
data class StepData(val startTime: String, val endTime: String, val count: Long)

/**
 * Repository for fetching step data from Health Connect and posting it to a server.
 *
 * @param healthConnectClient The HealthConnectClient instance (mocked in tests).
 * @param client The OkHttpClient instance (mocked in tests).
 */
class StepRepository(
    private val healthConnectClient: HealthConnectClient,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val endpoint = "https://testing.api.onvy.health/live"
    private val bearerToken = "ed95c82d6cde1c55686c94057a243ec2c416cbe6618006c4a8c41003f025172e"
    private val gson = Gson()

    /**
     * Secondary constructor for production use, creates real clients from context.
     */
    constructor(context: Context) : this(
        HealthConnectClient.getOrCreate(context),
        OkHttpClient()
    )

    /**
     * Fetches today's step data as a list of StepData.
     */
    suspend fun fetchTodaySteps(): List<StepData> = withContext(Dispatchers.IO) {
        val records = fetchTodayStepsRecord()
        records.map { it.toStepData() }
    }

    /**
     * Fetches today's total step count using the aggregate API (deduplicated, matches Health Connect UI).
     */
    suspend fun fetchTodayTotalSteps(): Long? = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        val aggregateRequest = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
        )
        val response = healthConnectClient.aggregate(aggregateRequest)
        response[StepsRecord.COUNT_TOTAL]
    }

    /**
     * Posts the given step data to the server as JSON.
     * Returns Result.success(message) or Result.failure(exception).
     */
    suspend fun postStepsData(steps: List<StepData>): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (steps.isEmpty()) return@withContext Result.failure(Exception("No steps found for today."))
            val jsonBody = stepDataListToJson(steps)
            val request = buildPostRequest(jsonBody)
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Success: ${response.code}")
            } else {
                Result.failure(Exception("Failed: ${response.code} ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches today's StepsRecord from Health Connect.
     */
    private suspend fun fetchTodayStepsRecord(): List<StepsRecord> {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records
    }

    /**
     * Maps a StepsRecord to StepData.
     */
    private fun StepsRecord.toStepData(): StepData =
        StepData(
            startTime = this.startTime.toString(),
            endTime = this.endTime.toString(),
            count = this.count
        )

    /**
     * Converts a list of StepData to a JSON string for the server using Gson.
     */
    private fun stepDataListToJson(steps: List<StepData>): String {
        return gson.toJson(mapOf("steps" to steps))
    }

    /**
     * Builds the OkHttp POST request for the step data JSON.
     */
    private fun buildPostRequest(jsonBody: String): Request =
        Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $bearerToken")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
} 
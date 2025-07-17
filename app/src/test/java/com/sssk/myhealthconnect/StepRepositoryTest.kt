package com.sssk.myhealthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.request.ReadRecordsRequest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import okhttp3.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
class StepRepositoryTest {
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var repository: StepRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Before
    fun setup() {
        healthConnectClient = mockk(relaxed = true)
        okHttpClient = mockk(relaxed = true)
        repository = StepRepository(healthConnectClient, okHttpClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `fetchTodaySteps returns mapped StepData`() = runTest {
        val fakeRecord = StepsRecord(
            startTime = Instant.parse("2024-06-01T00:00:00Z"),
            startZoneOffset = null,
            endTime = Instant.parse("2024-06-01T01:00:00Z"),
            endZoneOffset = null,
            count = 100
        )
        val response = ReadRecordsResponse(listOf(fakeRecord), null)
        coEvery { healthConnectClient.readRecords(any<ReadRecordsRequest<StepsRecord>>()) } returns response

        val result = repository.fetchTodaySteps()
        testDispatcher.scheduler.advanceUntilIdle()
        assert(result.size == 1)
        assert(result[0].count == 100L)
    }

    @Test
    fun `postStepsData returns success on 200 response`() = runTest {
        val step = StepData("2024-06-01T00:00:00Z", "2024-06-01T01:00:00Z", 100)
        val response = mockk<Response> {
            every { isSuccessful } returns true
            every { code } returns 200
        }
        every { okHttpClient.newCall(any()).execute() } returns response

        val result = repository.postStepsData(listOf(step))
        testDispatcher.scheduler.advanceUntilIdle()
        println("Result: $result")
        if (result.isFailure) {
            println("Exception: ${result.exceptionOrNull()}")
        }
        assert(result.isSuccess)
        assert(result.getOrNull()?.contains("Success") == true)
    }
} 
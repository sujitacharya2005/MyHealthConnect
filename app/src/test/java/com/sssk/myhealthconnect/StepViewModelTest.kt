package com.sssk.myhealthconnect

import android.app.Application
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
class StepViewModelTest {
    private lateinit var repository: StepRepository
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var viewModel: StepViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUpDispatcher() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        healthConnectManager = mockk(relaxed = true)
        viewModel = StepViewModel(repository)
    }

    @After
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkPermissions updates permissionsGranted`() = runTest {
        coEvery { healthConnectManager.hasAllPermissions(any()) } returns true
        viewModel.checkPermissions(healthConnectManager)
        testDispatcher.scheduler.advanceUntilIdle()
        assert(viewModel.permissionsGranted.value)
    }

    @Test
    fun `fetchAndPostSteps updates message on success`() = runTest {
        val steps = listOf(StepData("start", "end", 100))
        coEvery { repository.fetchTodaySteps() } returns steps
        coEvery { repository.postStepsData(steps) } returns Result.success("Success: 200")
        viewModel.fetchAndPostSteps(healthConnectManager)
        testDispatcher.scheduler.advanceUntilIdle()
        assert(viewModel.message.value.contains("Success"))
    }
} 
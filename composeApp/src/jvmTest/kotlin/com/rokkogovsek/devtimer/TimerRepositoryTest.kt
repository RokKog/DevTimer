package com.rokkogovsek.devtimer

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.rokkogovsek.devtimer.database.DevTimerDatabase
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TimerRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: TimerRepository

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            properties = Properties()
        )
        DevTimerDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        repository = TimerRepository(DevTimerDatabase(driver))
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `returns the same project when a directory is registered twice`() {
        withTemporaryDirectory { directory ->
            val firstId = repository.getOrCreateProject(directory)
            val secondId = repository.getOrCreateProject(directory)

            assertEquals(firstId, secondId)
            assertEquals(1, repository.getProjects().size)
            assertEquals(directory.name, repository.getProjects().single().name)
        }
    }

    @Test
    fun `stores and aggregates added and removed lines by day`() {
        withTemporaryDirectory { directory ->
            val firstStart = LocalDateTime.of(2026, 7, 13, 9, 0)
            repository.saveSession(
                directory = directory,
                startTime = firstStart,
                endTime = firstStart.plusMinutes(25),
                durationSeconds = (25 * SECONDS_PER_MINUTE).toLong(),
                startLineCount = 10,
                endLineCount = 18
            )

            val secondStart = LocalDateTime.of(2026, 7, 13, 14, 0)
            repository.saveSession(
                directory = directory,
                startTime = secondStart,
                endTime = secondStart.plusMinutes(25),
                durationSeconds = (25 * SECONDS_PER_MINUTE).toLong(),
                startLineCount = 18,
                endLineCount = 15
            )

            val row = repository.getChangedLinesByDay(month = 7, year = 2026).single()
            assertEquals(13, row.day)
            assertEquals(8, row.addedLines)
            assertEquals(3, row.removedLines)
            assertEquals(11, row.changedLines)
            assertEquals(listOf(2026), repository.getYears())
        }
    }

    @Test
    fun `returns information about the latest session`() {
        withTemporaryDirectory { directory ->
            val firstStart = LocalDateTime.of(2026, 7, 12, 9, 0)
            repository.saveSession(
                directory = directory,
                startTime = firstStart,
                endTime = firstStart.plusMinutes(25),
                durationSeconds = (25 * SECONDS_PER_MINUTE).toLong(),
                startLineCount = 10,
                endLineCount = 12
            )

            val latestStart = LocalDateTime.of(2026, 7, 13, 9, 0)
            repository.saveSession(
                directory = directory,
                startTime = latestStart,
                endTime = latestStart.plusMinutes(45),
                durationSeconds = (45 * SECONDS_PER_MINUTE).toLong(),
                startLineCount = 12,
                endLineCount = 20
            )

            val latestSession = assertNotNull(repository.getLastSession())
            assertEquals(directory.name, latestSession.projectName)
            assertEquals(latestStart.plusMinutes(45), latestSession.endTime)
            assertEquals(8, latestSession.changedLines)
        }
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("devtimer-repository-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}

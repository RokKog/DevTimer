package com.rokkogovsek.devtimer

import androidx.compose.ui.graphics.Color
import com.rokkogovsek.devtimer.database.DevTimerDatabase
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class UiProject(
    val id: Long,
    val name: String,
    val color: Color
)

data class ChartRow(
    val day: Int,
    val projectName: String,
    val color: Color,
    val addedLines: Int,
    val removedLines: Int,
    val changedLines: Int
)

data class LastSessionInfoData(
    val projectName: String,
    val endTime: LocalDateTime,
    val changedLines: Int
)

class TimerRepository(
    private val database: DevTimerDatabase = DatabaseManager.database
) {
    private val developerQueries = database.developerQueries
    private val projectQueries = database.projectQueries
    private val sessionQueries = database.sessionQueries

    init {
        developerQueries.insertDeveloper("Developer")
    }

    fun getOrCreateProject(directory: File): Long {
        val existingProject = projectQueries
            .getProjectByPath(directory.absolutePath)
            .executeAsOneOrNull()

        if (existingProject != null) {
            return existingProject.id
        }

        val developer = developerQueries
            .getDeveloperByName("Developer")
            .executeAsOne()

        val color = colorForProject(projectQueries.getAllProjects().executeAsList().size)

        projectQueries.insertProject(
            developer_id = developer.id,
            name = directory.name,
            directory_path = directory.absolutePath,
            color = color
        )

        return projectQueries
            .getProjectByPath(directory.absolutePath)
            .executeAsOne()
            .id
    }

    fun saveSession(
        directory: File,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        durationSeconds: Long,
        startLineCount: Int,
        endLineCount: Int
    ) {
        val projectId = getOrCreateProject(directory)

        val changes = calculateLineChanges(startLineCount, endLineCount)

        sessionQueries.insertSession(
            project_id = projectId,
            start_time = toUnixTime(startTime),
            end_time = toUnixTime(endTime),
            duration_seconds = durationSeconds,
            start_line_count = startLineCount.toLong(),
            end_line_count = endLineCount.toLong(),
            added_lines = changes.addedLines.toLong(),
            removed_lines = changes.removedLines.toLong(),
            changed_lines = changes.changedLines.toLong()
        )
    }

    fun getProjects(): List<UiProject> {
        return projectQueries
            .getAllProjects()
            .executeAsList()
            .map { project ->
                UiProject(
                    id = project.id,
                    name = project.name,
                    color = parseColor(project.color)
                )
            }
    }

    fun getYears(): List<Int> {
        return sessionQueries
            .getYears()
            .executeAsList()
            .mapNotNull { value ->
                value?.toInt()
            }
    }

    fun getChangedLinesByDay(month: Int, year: Int): List<ChartRow> {
        return sessionQueries
            .getChangedLinesByDay(
                monthNumber = month.toLong(),
                yearNumber = year.toLong()
            )
            .executeAsList()
            .map { row ->
                ChartRow(
                    day = row.day?.toInt() ?: 0,
                    projectName = row.project_name,
                    color = parseColor(row.color),
                    addedLines = row.added_lines?.toInt() ?: 0,
                    removedLines = row.removed_lines?.toInt() ?: 0,
                    changedLines = row.changed_lines?.toInt() ?: 0
                )
            }
            .filter { row ->
                row.day > 0
            }
    }

    fun getLastSession(): LastSessionInfoData? {
        val last = sessionQueries
            .getLastSession()
            .executeAsOneOrNull()
            ?: return null

        return LastSessionInfoData(
            projectName = last.project_name,
            endTime = fromUnixTime(last.end_time),
            changedLines = last.changed_lines.toInt()
        )
    }

    private fun toUnixTime(value: LocalDateTime): Long {
        return value
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()
    }

    private fun fromUnixTime(value: Long): LocalDateTime {
        return Instant
            .ofEpochSecond(value)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    private fun colorForProject(index: Int): String {
        val colors = listOf(
            "#E53935",
            "#1E40FF",
            "#43A047",
            "#FF9800",
            "#8E24AA",
            "#00897B"
        )

        return colors[index % colors.size]
    }

    private fun parseColor(value: String): Color {
        val hex = value.removePrefix("#")
        val number = hex.toLong(16)

        return Color(
            red = ((number shr 16) and 255) / 255f,
            green = ((number shr 8) and 255) / 255f,
            blue = (number and 255) / 255f,
            alpha = 1f
        )
    }
}

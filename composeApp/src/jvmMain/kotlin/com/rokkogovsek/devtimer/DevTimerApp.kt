package com.rokkogovsek.devtimer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import javax.swing.JFileChooser
import kotlin.math.max

enum class ScreenTab {
    TIMER,
    STATISTICS
}

@Composable
fun TimerApp() {
    val repository = remember { TimerRepository() }

    var selectedDurationMinutes by remember { mutableStateOf(DEFAULT_SESSION_MINUTES) }
    val initialTime = selectedDurationMinutes * SECONDS_PER_MINUTE

    var selectedDirectory by remember { mutableStateOf<File?>(null) }
    var remainingSeconds by remember { mutableStateOf(initialTime) }
    var isRunning by remember { mutableStateOf(false) }
    var lineCountBeforeStart by remember { mutableStateOf<Int?>(null) }
    var sessionStartTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var selectedTab by remember { mutableStateOf(ScreenTab.TIMER) }
    var refreshKey by remember { mutableStateOf(0) }

    val today = LocalDate.now()
    var selectedMonth by remember { mutableStateOf(today.month) }
    var selectedYear by remember { mutableStateOf(today.year) }

    val projects = remember(refreshKey) {
        repository.getProjects()
    }

    val lastSession = remember(refreshKey) {
        repository.getLastSession()
    }

    val years = remember(refreshKey) {
        val databaseYears = repository.getYears()
        if (databaseYears.isEmpty()) {
            listOf(LocalDate.now().year)
        } else {
            databaseYears.plus(LocalDate.now().year).distinct().sorted()
        }
    }

    val chartRows = remember(selectedMonth, selectedYear, refreshKey) {
        repository.getChangedLinesByDay(
            month = selectedMonth.value,
            year = selectedYear
        )
    }

    val backgroundColor = Color(0xFFF2EAF8)
    val boxColor = Color(0xFFEADDF7)
    val borderColor = Color(0xFFD8C8EB)
    val darkColor = Color(0xFF2E1A63)
    val textColor = Color(0xFF5B4C81)
    val lightTextColor = Color(0xFF9A8FB4)
    val elapsedColor = Color(0xFFB8A7D9)

    val startEnabled = selectedDirectory != null && !isRunning
    val pauseEnabled = isRunning
    val resetEnabled = remainingSeconds < initialTime || isRunning

    fun finishSession() {
        val directory = selectedDirectory ?: return
        val startLines = lineCountBeforeStart ?: return
        val startTime = sessionStartTime ?: return

        val endTime = LocalDateTime.now()
        val endLines = countKotlinCodeLines(directory)

        repository.saveSession(
            directory = directory,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = initialTime.toLong(),
            startLineCount = startLines,
            endLineCount = endLines
        )

        lineCountBeforeStart = null
        sessionStartTime = null
        refreshKey++
    }

    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--

            if (remainingSeconds == 0) {
                finishSession()
                isRunning = false
                remainingSeconds = initialTime
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(min = 700.dp)
                .heightIn(min = 500.dp)
                .padding(16.dp)
        ) {
            Text(
                text = "Timer for Developers",
                fontSize = 24.sp,
                color = darkColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 18.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    ScreenTab.TIMER -> {
                        TimerScreen(
                            selectedDirectory = selectedDirectory,
                            remainingSeconds = remainingSeconds,
                            initialTime = initialTime,
                            startEnabled = startEnabled,
                            pauseEnabled = pauseEnabled,
                            resetEnabled = resetEnabled,
                            selectedDurationMinutes = selectedDurationMinutes,
                            durationOptions = SESSION_DURATION_OPTIONS_MINUTES,
                            durationSelectionEnabled = !isRunning && lineCountBeforeStart == null,
                            boxColor = boxColor,
                            borderColor = borderColor,
                            darkColor = darkColor,
                            textColor = textColor,
                            lightTextColor = lightTextColor,
                            elapsedColor = elapsedColor,
                            lastSession = lastSession,
                            onDurationSelected = { minutes ->
                                if (!isRunning && lineCountBeforeStart == null) {
                                    selectedDurationMinutes = minutes
                                    remainingSeconds = minutes * SECONDS_PER_MINUTE
                                }
                            },
                            onDirectorySelected = { file ->
                                selectedDirectory = file
                                isRunning = false
                                remainingSeconds = initialTime
                                lineCountBeforeStart = null
                                sessionStartTime = null
                                repository.getOrCreateProject(file)
                                refreshKey++
                            },
                            onStartClick = {
                                val directory = selectedDirectory

                                if (directory != null) {
                                    if (remainingSeconds == initialTime) {
                                        lineCountBeforeStart = countKotlinCodeLines(directory)
                                        sessionStartTime = LocalDateTime.now()
                                        repository.getOrCreateProject(directory)
                                        refreshKey++
                                    }

                                    isRunning = true
                                }
                            },
                            onPauseClick = {
                                isRunning = false
                            },
                            onResetClick = {
                                isRunning = false
                                remainingSeconds = initialTime
                                lineCountBeforeStart = null
                                sessionStartTime = null
                            }
                        )
                    }

                    ScreenTab.STATISTICS -> {
                        StatisticsScreen(
                            projects = projects,
                            chartRows = chartRows,
                            years = years,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            onMonthSelected = { selectedMonth = it },
                            onYearSelected = { selectedYear = it },
                            boxColor = boxColor,
                            borderColor = borderColor,
                            darkColor = darkColor,
                            textColor = textColor,
                            lightTextColor = lightTextColor
                        )
                    }
                }
            }

            BottomTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                darkColor = darkColor,
                textColor = textColor,
                borderColor = borderColor,
                boxColor = boxColor
            )
        }
    }
}

@Composable
fun TimerScreen(
    selectedDirectory: File?,
    remainingSeconds: Int,
    initialTime: Int,
    startEnabled: Boolean,
    pauseEnabled: Boolean,
    resetEnabled: Boolean,
    selectedDurationMinutes: Int,
    durationOptions: List<Int>,
    durationSelectionEnabled: Boolean,
    boxColor: Color,
    borderColor: Color,
    darkColor: Color,
    textColor: Color,
    lightTextColor: Color,
    elapsedColor: Color,
    lastSession: LastSessionInfoData?,
    onDurationSelected: (Int) -> Unit,
    onDirectorySelected: (File) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResetClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val circleSize = min(maxWidth * 0.55f, maxHeight * 0.45f)
        val progress = remainingSeconds.toFloat() / initialTime.toFloat()

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProjectDirectoryCard(
                    selectedDirectory = selectedDirectory,
                    boxColor = boxColor,
                    borderColor = borderColor,
                    darkColor = darkColor,
                    textColor = textColor,
                    lightTextColor = lightTextColor,
                    onDirectorySelected = onDirectorySelected
                )

                ControlPanelCard(
                    startEnabled = startEnabled,
                    pauseEnabled = pauseEnabled,
                    resetEnabled = resetEnabled,
                    selectedDurationMinutes = selectedDurationMinutes,
                    durationOptions = durationOptions,
                    durationSelectionEnabled = durationSelectionEnabled,
                    darkColor = darkColor,
                    lightTextColor = lightTextColor,
                    boxColor = boxColor,
                    borderColor = borderColor,
                    onDurationSelected = onDurationSelected,
                    onStartClick = onStartClick,
                    onPauseClick = onPauseClick,
                    onResetClick = onResetClick,
                    showHelpText = selectedDirectory != null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(circleSize),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 18f
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )

                        drawArc(
                            color = elapsedColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = androidx.compose.ui.geometry.Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        drawArc(
                            color = darkColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = androidx.compose.ui.geometry.Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = formatTime(remainingSeconds),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkColor
                    )
                }
            }

            LastSessionInfo(
                lastSession = lastSession,
                textColor = textColor
            )
        }
    }
}

@Composable
fun LastSessionInfo(
    lastSession: LastSessionInfoData?,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (lastSession == null) {
                "Last session: /"
            } else {
                "Last session: ${lastSession.endTime.toLocalDate()} (${lastSession.projectName})"
            },
            fontSize = 14.sp,
            color = textColor
        )

        Text(
            text = if (lastSession == null) {
                "Lines changed: /"
            } else {
                "Lines changed: ${lastSession.changedLines}"
            },
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
fun RowScope.ProjectDirectoryCard(
    selectedDirectory: File?,
    boxColor: Color,
    borderColor: Color,
    darkColor: Color,
    textColor: Color,
    lightTextColor: Color,
    onDirectorySelected: (File) -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(boxColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Select project directory"
                }

                val result = chooser.showOpenDialog(null)

                if (result == JFileChooser.APPROVE_OPTION) {
                    onDirectorySelected(chooser.selectedFile)
                }
            }
            .padding(16.dp)
    ) {
        Text(
            text = "Project Directory",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = darkColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (selectedDirectory == null) {
                Text(
                    text = "Select a project directory",
                    fontSize = 14.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = selectedDirectory.absolutePath,
                    fontSize = 13.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text(
            text = if (selectedDirectory == null) "" else "Click to change project directory",
            fontSize = 11.sp,
            color = lightTextColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun RowScope.ControlPanelCard(
    startEnabled: Boolean,
    pauseEnabled: Boolean,
    resetEnabled: Boolean,
    selectedDurationMinutes: Int,
    durationOptions: List<Int>,
    durationSelectionEnabled: Boolean,
    darkColor: Color,
    lightTextColor: Color,
    boxColor: Color,
    borderColor: Color,
    onDurationSelected: (Int) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResetClick: () -> Unit,
    showHelpText: Boolean
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(boxColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Control Panel",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = darkColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        SimpleDropdown(
            modifier = Modifier.fillMaxWidth(),
            selectedText = "$selectedDurationMinutes minutes",
            items = durationOptions,
            itemText = { "$it minutes" },
            boxColor = boxColor,
            borderColor = borderColor,
            textColor = darkColor,
            enabled = durationSelectionEnabled,
            onItemSelected = onDurationSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleButton(
                symbol = "▶",
                color = darkColor,
                enabled = startEnabled,
                onClick = onStartClick
            )

            CircleButton(
                symbol = "⏸",
                color = darkColor,
                enabled = pauseEnabled,
                onClick = onPauseClick
            )

            CircleButton(
                symbol = "↻",
                color = darkColor,
                enabled = resetEnabled,
                onClick = onResetClick
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (showHelpText) "Start, pause or reset the timer" else "",
            fontSize = 11.sp,
            color = lightTextColor
        )
    }
}

@Composable
fun CircleButton(
    symbol: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val currentColor = if (enabled) color else color.copy(alpha = 0.35f)

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(42.dp)
            .border(2.dp, currentColor, CircleShape)
    ) {
        Text(
            text = symbol,
            color = currentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatisticsScreen(
    projects: List<UiProject>,
    chartRows: List<ChartRow>,
    years: List<Int>,
    selectedMonth: Month,
    selectedYear: Int,
    onMonthSelected: (Month) -> Unit,
    onYearSelected: (Int) -> Unit,
    boxColor: Color,
    borderColor: Color,
    darkColor: Color,
    textColor: Color,
    lightTextColor: Color
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SimpleDropdown(
                modifier = Modifier.weight(1f),
                selectedText = monthName(selectedMonth),
                items = Month.entries.toList(),
                itemText = { monthName(it) },
                boxColor = boxColor,
                borderColor = borderColor,
                textColor = textColor,
                onItemSelected = onMonthSelected
            )

            SimpleDropdown(
                modifier = Modifier.weight(1f),
                selectedText = selectedYear.toString(),
                items = years,
                itemText = { it.toString() },
                boxColor = boxColor,
                borderColor = borderColor,
                textColor = textColor,
                onItemSelected = onYearSelected
            )
        }

        Legend(
            projects = projects,
            textColor = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF7F1FB))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (projects.isEmpty() || chartRows.isEmpty()) {
                Text(
                    text = "No sessions for selected month.",
                    color = lightTextColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                StatisticsChart(
                    projects = projects,
                    chartRows = chartRows,
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    darkColor = darkColor
                )
            }
        }
    }
}

@Composable
fun StatisticsChart(
    projects: List<UiProject>,
    chartRows: List<ChartRow>,
    selectedMonth: Month,
    selectedYear: Int,
    darkColor: Color
) {
    val daysInMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()

    val bars = (1..daysInMonth).map { day ->
        val values = projects.map { project ->
            val sumForDay = chartRows
                .filter { row ->
                    row.day == day && row.projectName == project.name
                }
                .sumOf { row -> row.changedLines }

            Bars.Data(
                label = project.name,
                value = sumForDay.toDouble(),
                color = SolidColor(project.color)
            )
        }

        Bars(
            label = day.toString(),
            values = values
        )
    }

    val maxValueFromDatabase = chartRows.maxOfOrNull { it.changedLines } ?: 0

    ColumnChart(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        data = bars,
        maxValue = max(50.0, maxValueFromDatabase.toDouble()),
        barProperties = BarProperties(
            thickness = 9.dp,
            spacing = 2.dp,
            cornerRadius = Bars.Data.Radius.Circular(4.dp)
        )
    )
}

@Composable
fun Legend(
    projects: List<UiProject>,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        projects.forEach { project ->
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(project.color)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = project.name,
                    fontSize = 12.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun <T> SimpleDropdown(
    modifier: Modifier = Modifier,
    selectedText: String,
    items: List<T>,
    itemText: (T) -> String,
    boxColor: Color,
    borderColor: Color,
    textColor: Color,
    enabled: Boolean = true,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentTextColor = if (enabled) textColor else textColor.copy(alpha = 0.45f)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(boxColor)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedText,
                color = currentTextColor,
                fontSize = 14.sp
            )

            Text(
                text = "▾",
                color = currentTextColor,
                fontSize = 14.sp
            )
        }

        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                ) {
                    Text(text = itemText(item))
                }
            }
        }
    }
}

@Composable
fun BottomTabs(
    selectedTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    darkColor: Color,
    textColor: Color,
    borderColor: Color,
    boxColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(boxColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTabItem(
            modifier = Modifier.weight(1f),
            icon = "⏱",
            title = "Timer",
            selected = selectedTab == ScreenTab.TIMER,
            selectedColor = darkColor,
            normalColor = textColor,
            onClick = { onTabSelected(ScreenTab.TIMER) }
        )

        BottomTabItem(
            modifier = Modifier.weight(1f),
            icon = "▥",
            title = "Statistics",
            selected = selectedTab == ScreenTab.STATISTICS,
            selectedColor = darkColor,
            normalColor = textColor,
            onClick = { onTabSelected(ScreenTab.STATISTICS) }
        )
    }
}

@Composable
fun BottomTabItem(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    selected: Boolean,
    selectedColor: Color,
    normalColor: Color,
    onClick: () -> Unit
) {
    val color = if (selected) selectedColor else normalColor.copy(alpha = 0.65f)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            color = color,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            text = title,
            color = color,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

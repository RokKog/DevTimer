package com.rokkogovsek.devtimer

import kotlin.math.abs

data class LineChangeSummary(
    val addedLines: Int,
    val removedLines: Int,
    val changedLines: Int
)

fun calculateLineChanges(startLineCount: Int, endLineCount: Int): LineChangeSummary {
    require(startLineCount >= 0) { "Starting line count cannot be negative." }
    require(endLineCount >= 0) { "Ending line count cannot be negative." }

    val difference = endLineCount - startLineCount

    return LineChangeSummary(
        addedLines = difference.coerceAtLeast(0),
        removedLines = (-difference).coerceAtLeast(0),
        changedLines = abs(difference)
    )
}

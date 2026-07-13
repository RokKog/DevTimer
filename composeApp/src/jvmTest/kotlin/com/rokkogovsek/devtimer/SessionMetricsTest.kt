package com.rokkogovsek.devtimer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionMetricsTest {

    @Test
    fun `calculates added lines`() {
        assertEquals(
            LineChangeSummary(addedLines = 8, removedLines = 0, changedLines = 8),
            calculateLineChanges(startLineCount = 10, endLineCount = 18)
        )
    }

    @Test
    fun `calculates removed lines`() {
        assertEquals(
            LineChangeSummary(addedLines = 0, removedLines = 6, changedLines = 6),
            calculateLineChanges(startLineCount = 18, endLineCount = 12)
        )
    }

    @Test
    fun `returns zero changes for equal line counts`() {
        assertEquals(
            LineChangeSummary(addedLines = 0, removedLines = 0, changedLines = 0),
            calculateLineChanges(startLineCount = 12, endLineCount = 12)
        )
    }

    @Test
    fun `rejects negative line counts`() {
        assertFailsWith<IllegalArgumentException> {
            calculateLineChanges(startLineCount = -1, endLineCount = 2)
        }
    }
}

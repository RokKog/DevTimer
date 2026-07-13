package com.rokkogovsek.devtimer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeFormatterTest {

    @Test
    fun `formats zero seconds`() {
        assertEquals("00:00", formatTime(0))
    }

    @Test
    fun `formats minutes and seconds with leading zeros`() {
        assertEquals("01:05", formatTime(65))
        assertEquals("25:00", formatTime(25 * SECONDS_PER_MINUTE))
    }

    @Test
    fun `supports durations longer than one hour`() {
        assertEquals("60:00", formatTime(60 * SECONDS_PER_MINUTE))
    }

    @Test
    fun `rejects negative durations`() {
        assertFailsWith<IllegalArgumentException> {
            formatTime(-1)
        }
    }
}

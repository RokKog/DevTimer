package com.rokkogovsek.devtimer

import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthFormatterTest {
    @Test
    fun `returns full English month name`() {
        assertEquals("January", monthName(Month.JANUARY))
        assertEquals("December", monthName(Month.DECEMBER))
    }
}

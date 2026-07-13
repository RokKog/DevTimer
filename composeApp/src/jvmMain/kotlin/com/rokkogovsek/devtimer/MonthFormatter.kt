package com.rokkogovsek.devtimer

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** Returns the full English display name of a month, for example "January". */
fun monthName(month: Month): String =
    month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

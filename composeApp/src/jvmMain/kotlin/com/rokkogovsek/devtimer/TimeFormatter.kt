package com.rokkogovsek.devtimer

fun formatTime(totalSeconds: Int): String {
    require(totalSeconds >= 0) { "Time cannot be negative." }

    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return "%02d:%02d".format(minutes, seconds)
}

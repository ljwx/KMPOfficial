package org.example.project.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object TimeUtils {

    @OptIn(ExperimentalTime::class)
    fun currentTimeMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
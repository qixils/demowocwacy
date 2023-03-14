package dev.qixils.demowocwacy

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Month
import java.time.ZoneOffset
import java.time.ZonedDateTime

object ElectionCycle {
    fun start() {
        runBlocking {
            Bot.jda.awaitReady()
            // init active decrees
            Bot.selectedDecrees.filter(Decree::persistent).forEach(Decree::execute)
            // loop
            while (true) {
                // abort after April 1st
                val zdt = ZonedDateTime.now(ZoneOffset.UTC)
                if (zdt.month == Month.APRIL && zdt.dayOfMonth > 1)
                    break
                // wait for top of hour to start
                val now = System.currentTimeMillis()
                val nextHour = now + 3600000 - now % 3600000
                delay(nextHour - now)
                // start election
                TODO()
            }
        }
    }
}
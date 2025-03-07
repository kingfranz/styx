package Styx

import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

///////////////////////////////////////////////////////////////////////////////

suspend inline fun timer(dur: Duration, pre: Boolean = false, block: () -> Unit) {
    timer(dur.toMillis(), pre, block)
}

suspend inline fun timer(ms: Long, pre: Boolean = false, block: () -> Unit) {
    if (pre)
        delay(ms)
    while (true) {
        val start = Instant.now()
        block()
        val stop = Instant.now()
        val diff = Duration.between(start, stop).toMillis()
        if (ms > diff)
            delay(ms - diff)
    }
}

///////////////////////////////////////////////////////////////////////////////

suspend fun main(): Unit = coroutineScope {
    // Set frame size
    try {
        val win = GameWin()
        win.run()
    }
    catch (e: Exception) {
        println("Main: ${e.message}")
        e.printStackTrace()
    }
    finally {
        println("Main: finally")
    }
}

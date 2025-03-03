import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min


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
    val width = 1040
    val height = 1040+52

    try {
        val win = GameWin(width, height)
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

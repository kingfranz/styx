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

class GameWin(val cWidth: Int, val cHeight: Int): JFrame() {
    val child = ClientArea()

    init {
        background = Color.WHITE
        isVisible = true
        setDefaultCloseOperation(EXIT_ON_CLOSE)
        add(child)
        pack()
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                when (e.keyChar) {
                    'q' -> System.exit(0)
                }
            }

            override fun keyPressed(e: KeyEvent) {
                //println("keyPressed: ${e.keyCode}")
                child.dispatchEvent(e)
            }

            override fun keyReleased(e: KeyEvent) {
                //println("keyReleased: ${e.keyCode}")
                child.dispatchEvent(e)
            }
        })
    }

    override fun getPreferredSize(): Dimension? {
        return Dimension(cWidth, cHeight)
    }

    override fun paint(g: Graphics) {
        super.paint(g)
        child.repaint()
    }

    suspend fun run(): Unit = coroutineScope {
        child.run()
    }
}

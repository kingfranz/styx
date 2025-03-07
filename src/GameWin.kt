package Styx

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

class GameWin(): JFrame() {
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

    override fun paint(g: Graphics) {
        try {
            super.paint(g)
            child.repaint()
        }
        catch (e: Exception) {
            println("GameWin.paint: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun run(): Unit = coroutineScope {
        try {
            child.run()
        }
        catch (e: Exception) {
            println("GameWin.run: ${e.message}")
            e.printStackTrace()
        }
        finally {
            println("GameWin.run: finally")
        }
    }
}

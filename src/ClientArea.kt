import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Button
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.LayoutManager
import java.awt.Point
import java.awt.Rectangle
import java.awt.Scrollbar
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.LayoutStyle
import javax.swing.border.EmptyBorder

///////////////////////////////////////////////////////////////////////////////

class ClientArea(): JPanel(), iArena {
    val resetButton = Button("Reset")
    val speedLbl = JLabel("10")
    val drawLbl = JLabel("")
    val locLbl = JLabel("")
    val arena = Arena(this)
    val topPanel = JPanel(FlowLayout())

    init {
        background = Color.GRAY
        layout = BorderLayout()
        speedLbl.font = Font("Arial", Font.BOLD, 30)
        speedLbl.border = EmptyBorder(0, 20, 0, 20)
        drawLbl.font = Font("Arial", Font.BOLD, 30)
        drawLbl.border = EmptyBorder(0, 0, 0, 20)
        locLbl.font = Font("Arial", Font.BOLD, 30)
        //speedCtrl.preferredSize = Dimension(400, 30)
        //speedCtrl.location = Point(0, 0)
        add(topPanel, BorderLayout.NORTH)
        topPanel.add(resetButton)
        topPanel.add(speedLbl)
        topPanel.add(drawLbl)
        topPanel.add(locLbl)
        add(arena, BorderLayout.CENTER)
        isVisible = true
//        speedCtrl.addAdjustmentListener {
//            arena.sprite.spriteSpeed = speedCtrl.value
//            speedLbl.text = speedCtrl.value.toString()
//        }
        resetButton.addActionListener {
           runBlocking { arena.reset() }
        }
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                when (e.keyChar) {
                    'q' -> System.exit(0)
                }
            }

            override fun keyPressed(e: KeyEvent) {
                //println("keyPressed-c: ${e.keyCode}")
                arena.dispatchEvent(e)
                if (arena.player.pos.drawing)
                    drawLbl.text = "Draw"
                else
                    drawLbl.text = ""
                drawLbl.repaint()
            }

            override fun keyReleased(e: KeyEvent) {
                //println("keyReleased-c: ${e.keyCode}")
                arena.dispatchEvent(e)
            }
        })
    }

    override fun showSpeed(value: Int) {
        speedLbl.text = value.toString()
    }

    override fun showDrawMode(active: Boolean) {
        drawLbl.text = if (active) "DRAW" else ""
    }

    override fun showLoc(value: String) {
        locLbl.text = value
    }

    override fun addLeg(p: Point) {
        TODO("Not yet implemented")
    }

    override fun clearAreas() {
        TODO("Not yet implemented")
    }


    override fun clearLines() {
        TODO("Not yet implemented")
    }

    override fun mkArea() {
        TODO("Not yet implemented")
    }

    override fun numLegs(): Int {
        TODO("Not yet implemented")
    }

    override fun getWalls(): Walls {
        TODO("Not yet implemented")
    }

    override fun isPosAvailable(p: Point): Boolean {
        TODO("Not yet implemented")
    }

    override fun paint(g: Graphics?) {
        super.paint(g)
        arena.repaint()
    }

    suspend fun run(): Unit = coroutineScope {
        arena.run()
    }
}

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.BasicStroke
import java.awt.Canvas
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.LinkedList

///////////////////////////////////////////////////////////////////////////////

class Arena(val parent: iArena): Canvas(), iArena, iTargets {
    val edgeSz = 20
    val sprite = Sprite(this, edgeSz)
    val player = Player(this)
    val keys = mutableMapOf<Int, Boolean>()
    val lines = LinkedList<Pair<Int, Int>>() // (x, y)
    var xArr: IntArray = IntArray(1000) { i -> i }
    var yArr: IntArray = IntArray(1000) { i -> i }
    var arrSize = 0
    val areas = LinkedList<Polygon>()

    init {
        isFocusable = true
        preferredSize = java.awt.Dimension(1000+2*edgeSz, 1000+2*edgeSz)
        minimumSize = java.awt.Dimension(1000+2*edgeSz, 1000+2*edgeSz)
        maximumSize = java.awt.Dimension(1000+2*edgeSz, 1000+2*edgeSz)
        size = java.awt.Dimension(1000+2*edgeSz, 1000+2*edgeSz)
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                when (e.keyChar) {
                    'q' -> System.exit(0)
                }
            }

            override fun keyPressed(e: KeyEvent) {
                //println("keyPressed-a: ${e.keyCode}")
                keys[e.keyCode] = true
                runBlocking {
                    when (e.keyCode) {
                        KeyEvent.VK_LEFT -> player.move(Player.Direction.LEFT,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true)
                        KeyEvent.VK_RIGHT -> player.move(Player.Direction.RIGHT,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true)
                        KeyEvent.VK_UP -> player.move(Player.Direction.UP,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true)
                        KeyEvent.VK_DOWN -> player.move(Player.Direction.DOWN,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true)
                    }
                }
            }

            override fun keyReleased(e: KeyEvent) {
                //println("keyReleased-a: ${e.keyCode}")
                keys[e.keyCode] = false
            }
        })
        requestFocus()
    }

    suspend fun reset() {
        sprite.reset()
        player.reset()
        clearLines()
        clearAreas()
    }

    override fun showSpeed(value: Int) {
        parent.showSpeed(value)
    }

    override fun showDrawMode(active: Boolean) {
        parent.showDrawMode(active)
    }

    override fun showLoc(value: String) {
        parent.showLoc(value)
    }

    override fun getLines(): Triple<Int, IntArray, IntArray> {
        return Triple(arrSize, xArr, yArr)
    }

    override fun getDrawnAreas(): LinkedList<Polygon> {
        return areas
    }

    override fun addLeg(p: Pair<Int, Int>) {
        if (arrSize > 0) {
            if (xArr[arrSize-1] == p.first && yArr[arrSize-1] == p.second) {
                return
            }
            if (arrSize > 1) {
                val x1 = xArr[arrSize - 2]
                val y1 = yArr[arrSize - 2]
                val x2 = xArr[arrSize - 1]
                val y2 = yArr[arrSize - 1]
                val x3 = p.first
                val y3 = p.second

                if ((y2 - y1) * (x3 - x2) == (y3 - y2) * (x2 - x1)) {
                    xArr[arrSize - 1] = p.first
                    yArr[arrSize - 1] = p.second
                    lines.removeLast()
                    lines.add(p)
                    return
                }
            }
        }
        if (arrSize == xArr.size) {
            xArr += IntArray(1000) { i -> i }
            yArr += IntArray(1000) { i -> i }
        }
        xArr[arrSize] = p.first
        yArr[arrSize] = p.second
        arrSize++
        lines.add(p)
    }

    override fun numLegs(): Int {
        return lines.size
    }

    override fun clearLines() {
        lines.clear()
    }

    override fun clearAreas() {
        areas.clear()
    }

    override fun mkArea() {
        val xs = IntArray(lines.size) { i -> lines[i].first }
        val ys = IntArray(lines.size) { i -> lines[i].second }
        val poly = Polygon(xs, ys, lines.size)
        areas.add(poly)
        lines.clear()
        arrSize = 0
    }

    fun drawAxis(g: Graphics2D) {
        g.color = Color.RED
        g.stroke = java.awt.BasicStroke(4.0f)
        g.drawLine(0, 0, 100, 0)
        g.color = Color.BLUE
        g.drawLine(0, 0, 0, 100)
    }

    fun drawEdge(g: Graphics2D) {
        g.color = Color.BLACK
        g.stroke = java.awt.BasicStroke(2.0f)
        g.drawLine(sprite.minX, sprite.minY, sprite.maxX, sprite.minY)
        g.drawLine(sprite.maxX, sprite.minY, sprite.maxX, sprite.maxY)
        g.drawLine(sprite.maxX, sprite.maxY, sprite.minX, sprite.maxY)
        g.drawLine(sprite.minX, sprite.maxY, sprite.minX, sprite.minY)
    }

    override fun update(g: Graphics) {
        runBlocking { sprite.step() }
    }

    override fun paint(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)

        val spriteGraph = g2d.create() as Graphics2D
        spriteGraph.translate(edgeSz, edgeSz)
        //spriteGraph.scale(1.0, -1.0)
        drawEdge(spriteGraph)
        runBlocking { sprite.draw(spriteGraph) }
        spriteGraph.dispose()

        val playerGraph = g2d.create() as Graphics2D
        playerGraph.translate(edgeSz, edgeSz)
        //playerGraph.scale((width-2*edgeSz)/1000.0, (height-2*edgeSz)/1000.0)
        runBlocking { player.draw(playerGraph) }

        val (num, xs, ys) = getLines()
        playerGraph.color = Color.RED
        playerGraph.stroke = java.awt.BasicStroke(2.0f)
        playerGraph.drawPolyline(xs, ys, num)

        // show areas
        for (a in areas) {
            playerGraph.color = Color.BLACK
            playerGraph.stroke = BasicStroke(1f)
            playerGraph.drawPolygon(a)
            playerGraph.color = Color.GRAY
            playerGraph.fillPolygon(a)
        }
        playerGraph.dispose()
    }

    suspend fun run(): Unit = coroutineScope {
        launch { player.jsProducer.joystick.run() }
        launch { player.jsProducer.runJoystick() }
        launch { player.jsProducer.makeMoves() }
        createBufferStrategy(2)
        val strategy = bufferStrategy
        launch(Dispatchers.Default + CoroutineName("Arena")) {
            while(true) {
                delay(sprite.spriteSpeed.toLong())
                do {
                    do {
                        val g = strategy.drawGraphics
                        try {
                            update(g)
                            paint(g)
                        } finally {
                            g.dispose()
                        }
                    } while (strategy.contentsRestored())
                    strategy.show()
                } while (strategy.contentsLost())
            }
        }
    }
}

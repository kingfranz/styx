import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.util.LinkedList
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import java.awt.Canvas
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.time.Duration
import java.time.Instant
import javax.swing.JFrame
import javax.swing.JPanel

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

class SpriteSegment(val x1: Int,
                    val y1: Int,
                    val x2: Int,
                    val y2: Int,
                    val color: Color = Color.BLACK)

///////////////////////////////////////////////////////////////////////////////

class Sprite(width: Int, height: Int) {
    val spriteLength = 10
    val spriteSize = 250
    val stepSize = 10
    val spriteSpeed: Int = 20 // per second
    val maxAngle = Math.toRadians(30.0)
    val tail = LinkedList<SpriteSegment>()
    val tailMutex = Mutex(false)
    var currentPoint = Point(0, 0)
    var currentAngle = 0.0
    val edgeSz = 20
    val minX = width / -2 + edgeSz
    val minY = height / -2 + edgeSz
    val maxX = width / 2 - edgeSz
    val maxY = height / 2 - edgeSz

    fun newAngle(a: Double): Double {
        return (a + Math.random() * 2 * maxAngle - maxAngle) % (2 * Math.PI)
    }

    fun newPoint(p: Point, a: Double, d: Int): Point {
        val x = p.x + (Math.cos(a) * d).toInt()
        val y = p.y + (Math.sin(a) * d).toInt()
        return Point(x, y)
    }

    fun mkSegment(p: Point, a: Double, d: Int, clr: Color): SpriteSegment {
        val p1 = newPoint(p, a+Math.PI/4, d/2)
        val p2 = newPoint(p, a-Math.PI/4, d/2)
        return SpriteSegment(p1.x, p1.y, p2.x, p2.y, clr)
    }

    fun rndClr(): Color {
        val r = (Math.random() * 255).toInt()
        val g = (Math.random() * 255).toInt()
        val b = (Math.random() * 255).toInt()
        return Color(r, g, b)
    }

    fun normalizeAngle(a: Double): Double {
        var na = a
        while (na < 0) {
            na += 2 * Math.PI
        }
        while (na > 2 * Math.PI) {
            na -= 2 * Math.PI
        }
        return na
    }

    suspend fun store() {
        tailMutex.lock()
        tail.addFirst(mkSegment(currentPoint, currentAngle, spriteSize, rndClr()))
        if (tail.size > spriteLength) {
            tail.removeLast()
        }
        tailMutex.unlock()
    }

    fun getEq(p1: Point, p2: Point): Pair<Double, Double> {
        val a = (p2.y - p1.y).toDouble() / (p2.x - p1.x).toDouble()
        val b = p1.y - a * p1.x
        return Pair(a, b)
    }

    fun handleWall(): Boolean {
        val left = newPoint(currentPoint, normalizeAngle(currentAngle + Math.PI/2), spriteSize/2)
        val right = newPoint(currentPoint, normalizeAngle(currentAngle - Math.PI/2), spriteSize/2)
        if (left.x > minX && left.x < maxX && left.y > minY && left.y < maxY &&
            right.x > minX && right.x < maxX && right.y > minY && right.y < maxY) {
            return false
        }
        try {
            when {
                left.x <= minX -> {
                    if(currentAngle > Math.PI / 2 && currentAngle < Math.PI * 1.5)
                        currentAngle = normalizeAngle(Math.PI - currentAngle)
                    else
                        return false
                }

                left.x >= maxX -> {
                    if(currentAngle < Math.PI / 2 || currentAngle > Math.PI * 1.5)
                        currentAngle = normalizeAngle(Math.PI - currentAngle)
                    else
                        return false
                }

                left.y <= minY -> {
                    if(currentAngle > Math.PI && currentAngle < Math.PI*2)
                        currentAngle = normalizeAngle(Math.PI - (currentAngle - Math.PI))
                    else
                        return false
                }

                left.y >= maxY -> {
                    if(currentAngle > 0 && currentAngle < Math.PI)
                        currentAngle = normalizeAngle(Math.PI * 2 - currentAngle)
                    else
                        return false
                }

                right.x <= minX -> {
                    if(currentAngle > Math.PI / 2 && currentAngle < Math.PI * 1.5)
                        currentAngle = normalizeAngle(Math.PI - currentAngle)
                    else
                        return false
                }

                right.x >= maxX -> {
                    if(currentAngle < Math.PI / 2 || currentAngle > Math.PI * 1.5)
                        currentAngle = normalizeAngle(Math.PI - currentAngle)
                    else
                        return false
                }

                right.y <= minY -> {
                    if(currentAngle > Math.PI && currentAngle < Math.PI*2)
                        currentAngle = normalizeAngle(Math.PI - (currentAngle - Math.PI))
                    else
                        return false
                }

                right.y >= maxY -> {
                    if(currentAngle > 0 && currentAngle < Math.PI)
                        currentAngle = normalizeAngle(Math.PI * 2 - currentAngle)
                    else
                        return false
                }
            }
            return true
        }
        catch (e: AssertionError) {
            println("Assertion failed: ${e.message}")
            return false
        }
    }

    suspend fun step() {
        if(!handleWall()) {
            currentAngle = normalizeAngle(newAngle(currentAngle))
        }
        currentPoint = newPoint(currentPoint, currentAngle, stepSize)
        store()
    }

    suspend fun draw(g: Graphics2D) {
        g.clipRect(minX, minY, maxX-minX, maxY-minY)
        g.stroke = java.awt.BasicStroke(2.0f)
        tailMutex.lock()
        for (segment in tail) {
            g.color = segment.color
            g.drawLine(segment.x1, segment.y1, segment.x2, segment.y2)
        }
        tailMutex.unlock()
    }
}

///////////////////////////////////////////////////////////////////////////////

class Arena(val aWidth: Int, val aHeight: Int): Canvas() {
    val sprite = Sprite(aWidth, aHeight)

    override fun getPreferredSize(): Dimension? {
        return Dimension(aWidth, aHeight)
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
        g2d.translate(width/2, height/2)
        g2d.scale(1.0, -1.0)
        drawEdge(g2d)
        runBlocking { sprite.draw(g2d) }
        g2d.dispose()
    }

    suspend fun run(): Unit = coroutineScope {
        createBufferStrategy(2)
        val strategy = bufferStrategy
        launch(Dispatchers.Default + CoroutineName("clock.tick")) {
            timer(Duration.ofMillis(1000L / sprite.spriteSpeed)) {
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

///////////////////////////////////////////////////////////////////////////////

class GameWin(val cWidth: Int, val cHeight: Int): JFrame() {
    val arena = Arena(cWidth, cHeight)

    init {
        background = Color.WHITE
        //setSize(cWidth, cHeight)
        isVisible = true
        setDefaultCloseOperation(EXIT_ON_CLOSE)
        add(arena)
        pack()
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                when (e.keyChar) {
                    'q' -> System.exit(0)
                }
            }

            override fun keyPressed(e: KeyEvent) {
                println("keyPressed: ${e.keyChar.toInt()}")
            }

            override fun keyReleased(e: KeyEvent) {
                println("keyReleased: ${e.keyChar.toInt()}")
            }
        })
    }

    override fun getPreferredSize(): Dimension? {
        return Dimension(cWidth, cHeight)
    }

    override fun paint(g: Graphics) {
        super.paint(g)
        arena.repaint()
    }

    suspend fun run(): Unit = coroutineScope {
        arena.run()
    }
}

///////////////////////////////////////////////////////////////////////////////

suspend fun main(): Unit = coroutineScope {
    // Set frame size
    val width = 800
    val height = 600

    // Add a label to the frame
    val win = GameWin(width, height)
    win.run()
}

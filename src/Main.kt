import java.awt.Canvas
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.Point2D
import java.awt.image.BufferStrategy
import java.util.LinkedList
import javax.swing.JFrame
import javax.swing.RepaintManager
import javax.swing.SwingConstants
import kotlin.math.cos
import kotlin.math.sin

class SpriteSegment(val x1: Int, val y1: Int, val x2: Int, val y2: Int, val color: Color = Color.BLACK)

class Sprite(val width: Int, val height: Int) {
    val spriteLength = 5
    val spriteSize = 150
    val stepSize = 20
    val maxAngle = Math.toRadians(30.0)
    val tail = LinkedList<SpriteSegment>()
    var currentPoint = Point(0, 0)
    var currentAngle = 0.0
    val minX = width / -2
    val minY = height / -2
    val maxX = width / 2
    val maxY = height / 2

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

    fun store() {
        tail.addFirst(mkSegment(currentPoint, currentAngle, spriteSize, rndClr()))
        if (tail.size > spriteLength) {
            tail.removeLast()
        }
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

    fun step() {
        if(!handleWall()) {
            currentAngle = normalizeAngle(newAngle(currentAngle))
        }
        currentPoint = newPoint(currentPoint, currentAngle, stepSize)
        store()
    }

    fun drawAxis(g: Graphics2D) {
        g.color = Color.RED
        g.stroke = java.awt.BasicStroke(4.0f)
        g.drawLine(0, 0, 100, 0)
        g.color = Color.BLUE
        g.drawLine(0, 0, 0, 100)
    }

    fun draw(g: Graphics2D) {
        //drawAxis(g)
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        g.color = Color.BLACK
        g.stroke = java.awt.BasicStroke(2.0f)
        for (segment in tail) {
            g.drawLine(segment.x1, segment.y1, segment.x2, segment.y2)
        }
        //val ff = tail.first()
        //println("${ff.x1} ${ff.y1} ${ff.x2} ${ff.y2}")
    }
}

class Field(cWidth: Int, cHeight: Int): Canvas() {
    val sprite = Sprite(cWidth, cHeight)

    init {
        background = Color.WHITE
        setSize(cWidth, cHeight)

    }

    override fun paint(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.translate(width/2, height/2)
        g2d.scale(1.0, -1.0)
        sprite.step()
        sprite.draw(g2d)
    }
}

fun main() {
    // Create a new JFrame instance
    val frame = JFrame("Basic JFrame Example")

    // Set the default close operation
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    // Set frame size
    val width = 800
    val height = 600
    frame.setSize(width, height)
    RepaintManager.currentManager(frame).isDoubleBufferingEnabled = true

    // Add a label to the frame
    val canvas = Field(width, height)
    RepaintManager.currentManager(canvas).isDoubleBufferingEnabled = true
    frame.add(canvas)

    // Make the frame visible
    frame.isVisible = true

    canvas.createBufferStrategy(2)
    val strategy = canvas.bufferStrategy
    while(true) {
        do {
            do {
                val g = strategy.drawGraphics
                try {
                    canvas.repaint()
                    Thread.sleep(50)
                } finally {
                    g.dispose()
                }
            } while (strategy.contentsRestored())
            strategy.show()
        } while (strategy.contentsLost())
    }
}
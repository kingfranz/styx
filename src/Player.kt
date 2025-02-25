import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import kotlin.math.hypot

///////////////////////////////////////////////////////////////////////////////

class Player(val parent: iArena) {

    enum class Direction { UP, RIGHT, DOWN, LEFT }

    fun Point.translate(p: Point): Point {
        return Point(x + p.x, y + p.y)
    }

    fun Point.magnitude(): Int {
        return hypot(x.toDouble(), y.toDouble()).toInt()
    }

    fun Point.direction(): Direction {
        return when {
            x > 0 -> Direction.RIGHT
            x < 0 -> Direction.LEFT
            y > 0 -> Direction.DOWN
            y < 0 -> Direction.UP
            else -> Direction.RIGHT
        }
    }

    fun mkVec(direction: Direction, dist: Int): Point {
        return when (direction) {
            Direction.UP -> Point(0, -dist)
            Direction.DOWN -> Point(0, dist)
            Direction.RIGHT -> Point(dist, 0)
            Direction.LEFT -> Point(-dist, 0)
        }
    }

    inner class PlayerPos(val arena: iArena) {
        private var isDrawing: Boolean = false
        private var position = Point(500, 0)
        private var drawDirection: Direction = Direction.RIGHT
        val mutex = Mutex(false)

        suspend fun reset() {
            mutex.withLock {
                isDrawing = false
                position = Point(500, 0)
                drawDirection = Direction.RIGHT
            }
        }

        val drawing: Boolean
            get() {
                return isDrawing
            }

        val posStr: String
            get() {
                return "${position.x} ${position.y}"
            }

        fun xy(): Point {
            return position
        }

        fun currentWall(): Wall? {
            return arena.getWalls().findWall(Point(position.x, position.y))
        }

        fun onEdge(): Boolean {
            return currentWall() != null
        }

        fun startDraw() {
            assert(!isDrawing)
            isDrawing = true
        }

        fun endDraw() {
            assert(isDrawing)
            isDrawing = false
        }

        fun updatePos(delta: Point) {
            val currentWall = currentWall()
            val newPos = position.translate(delta)
            if (currentWall!!.contains(newPos)) {
                position = newPos
                return
            }
        }

        fun updateDrawXY(delta: Point): Boolean {
            drawDirection = delta.direction()
            position = position.translate(delta)
            return !onEdge()
        }
    }

    var slowSpeed: Int = 1
    var normalSpeed: Int = 2
    var fastSpeed: Int = 3
    val pos = PlayerPos(parent)
    val joystick = Joystick()

    suspend fun reset() {
        pos.reset()
    }

    fun validateMove(delta: Point): Point {
        for (i in delta.magnitude() downTo 1) {
            if (parent.isPosAvailable(pos.xy().translate(delta))) {
                return delta
            }
        }
        return Point(0, 0)
    }

    suspend fun move(direction: Direction, special: Boolean, drawKey: Boolean) {
        pos.mutex.withLock {
            val speed = if (pos.drawing) {
                if (special) slowSpeed else normalSpeed
            } else {
                if (special) fastSpeed else normalSpeed
            }
            parent.showSpeed(speed)
            val delta = mkVec(direction, speed)
            if (pos.drawing) {
                //
                val realSpeed = validateMove(delta)
                if (pos.onEdge()) {
                    if (parent.numLegs() > 1) {
                        // we're on the edge
                        parent.addLeg(pos.xy())
                        pos.endDraw()
                        parent.showDrawMode(false)
                        parent.mkArea()
                    } else {
                        // false start
                        parent.clearLines()
                    }
                } else {
                    // we're making a turn
                    parent.addLeg(pos.xy())
                    if (!pos.updateDrawXY(realSpeed)) {
                        // we're on the edge
                        parent.addLeg(pos.xy())
                        pos.endDraw()
                        parent.showDrawMode(false)
                        parent.mkArea()
                    }
                }
            } else {
                // just moving along the edge
                parent.showDrawMode(false)
                if (drawKey && validateMove(delta) != Point(0, 0)) {
                    // start drawing
                    pos.startDraw()
                    parent.showDrawMode(true)
                    parent.addLeg(pos.xy())
                    pos.updateDrawXY(validateMove(delta))
                } else {
                    pos.updatePos(delta)
                }
            }
        }
    }

    suspend fun makeMoves(): Unit = coroutineScope {
        launch {
            while (true) {
                if (joystick.jsDir != null) {
                    move(joystick.jsDir!!, joystick.special, joystick.draw)
                }
                delay(20)
            }
        }
    }

    suspend fun draw(g: Graphics2D) {
        pos.mutex.withLock {
            // show player
            g.color = Color.RED
            g.fillRect(pos.xy().x - 10, pos.xy().y - 10, 20, 20)
            // show position
            parent.showLoc(pos.posStr)
        }
    }
}


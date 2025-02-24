import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Polygon
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min
import net.java.games.input.*

///////////////////////////////////////////////////////////////////////////////

class Player(val parent: iArena) {

    enum class Direction { UP, RIGHT, DOWN, LEFT }

    class PlayerPos {
        val topSide = Pair(0, 999)
        val rightSide = Pair(1000, 1999)
        val bottomSide = Pair(2000, 2999)
        val leftSide = Pair(3000, 3999)
        private var position: Int? = 500  // null when drawing
        private var isDrawing: Boolean = false
        private var drawPosition: Pair<Int, Int>? = null
        private var drawDirection: Direction? = null
        val mutex = Mutex(false)

        suspend fun reset() {
            mutex.withLock {
                position = 500
                isDrawing = false
                drawPosition = null
                drawDirection = null
            }
        }

        val drawing: Boolean
            get() {
                return isDrawing
            }

        val posStr: String
            get() {
                return if (position != null) position.toString() else "${drawPosition!!.first} ${drawPosition!!.second}"
            }

        fun xy(): Pair<Int, Int> {
            if (drawPosition != null)
                return drawPosition!!
            return pos2XY()
        }

        fun currentSide(): Direction {
            return when (position) {
                in topSide.first..topSide.second -> Direction.UP
                in rightSide.first..rightSide.second -> Direction.RIGHT
                in bottomSide.first..bottomSide.second -> Direction.DOWN
                in leftSide.first..leftSide.second -> Direction.LEFT
                else -> throw Exception("Invalid position $position")
            }
        }

        fun onEdge(): Boolean {
            val (x, y) = pos2XY()
            return x == 0 || x == 1000 || y == 0 || y == 1000
        }

        fun pos2XY(): Pair<Int, Int> {
            if (position == null) {
                if (drawPosition == null)
                    throw Exception("pos and XY null")
                return drawPosition!!
            }
            return when (currentSide()) {
                Direction.UP -> Pair(position!!, 0)
                Direction.RIGHT -> Pair(1000, position!! - 1000)
                Direction.DOWN -> Pair(1000 - (position!! - 2000), 1000)
                Direction.LEFT -> Pair(0, 1000 - (position!! - 3000))
            }
        }

        fun xy2Pos(): Int {
            if (drawPosition == null)
                throw Exception("xy2Pos: not drawing")
            when {
                drawPosition!!.first == 0 -> return 4000 - drawPosition!!.second
                drawPosition!!.first == 1000 -> return 1000 + drawPosition!!.second
                drawPosition!!.second == 0 -> return drawPosition!!.first
                drawPosition!!.second == 1000 -> return 3000 - drawPosition!!.first
                else -> throw Exception("xy2Pos: not on edge $drawPosition")
            }
        }

        fun startDraw() {
            assert(position != null && drawPosition == null && !isDrawing)
            isDrawing = true
            drawPosition = pos2XY()
            drawDirection = null
            position = null
        }

        fun endDraw() {
            assert(position == null && drawPosition != null && isDrawing)
            isDrawing = false
            position = xy2Pos()
            drawPosition = null
            drawDirection = null
        }

        fun updatePos(delta: Int, key: Direction) {
            //println("$position, $delta")
            when (currentSide()) {
                Direction.UP -> {
                    when (key) {
                        Direction.UP -> return
                        Direction.RIGHT -> position = min(position!! + delta, 1000)
                        Direction.DOWN -> position = if (position == 0) 4000 - delta else position
                        Direction.LEFT -> position = max(position!! - delta, 0)
                    }
                }

                Direction.RIGHT -> {
                    when (key) {
                        Direction.UP -> position = max(position!! - delta, 1000)
                        Direction.RIGHT -> return
                        Direction.DOWN -> position = min(position!! + delta, 2000)
                        Direction.LEFT -> position = if (position == 1000) 1000 - delta else position
                    }
                }

                Direction.DOWN -> {
                    when (key) {
                        Direction.UP -> position = if (position == 2000) 2000 - delta else position
                        Direction.RIGHT -> position = max(position!! - delta, 2000)
                        Direction.DOWN -> return
                        Direction.LEFT -> position = min(position!! + delta, 3000)
                    }
                }

                Direction.LEFT -> {
                    when (key) {
                        Direction.UP -> position = if (position!! + delta > 3999) 0 else position!! + delta
                        Direction.RIGHT -> position = if (position == 3000) 3000 - delta else position
                        Direction.DOWN -> position = max(position!! - delta, 3000)
                        Direction.LEFT -> return
                    }
                }
            }
        }

        fun updateDrawXY(direction: Direction, speed: Int): Boolean {
            var (newX, newY) = xy()
            when (direction) {
                Direction.UP -> newY -= speed
                Direction.DOWN -> newY += speed
                Direction.RIGHT -> newX += speed
                Direction.LEFT -> newX -= speed
            }
            drawDirection = direction
            drawPosition = Pair(newX, newY)
            return !onEdge()
        }

        fun clampSpeed(direction: Direction, speed: Int): Int {
            var (tmpX, tmpY) = xy()
            return when (direction) {
                Direction.UP -> if (tmpY - speed < 0) tmpY else speed
                Direction.DOWN -> if (tmpY + speed > 1000) 1000 - tmpY else speed
                Direction.RIGHT -> if (tmpX + speed > 1000) 1000 - tmpX else speed
                Direction.LEFT -> if (tmpX - speed < 0) tmpX else speed
            }
        }

        fun isOnGrid(direction: Direction): Boolean {
            return when (currentSide()) {
                Direction.UP -> return direction == Direction.DOWN
                Direction.DOWN -> return direction == Direction.UP
                Direction.RIGHT -> return direction == Direction.LEFT
                Direction.LEFT -> return direction == Direction.RIGHT
            }
        }
    }

    var slowSpeed: Int = 1
    var normalSpeed: Int = 2
    var fastSpeed: Int = 3
    val pos = PlayerPos()
    val jsProducer = JoystickData()

    suspend fun reset() {
        pos.reset()
    }

    suspend fun move(direction: Direction, special: Boolean, drawKey: Boolean) {
        pos.mutex.withLock {
            val speed = if (pos.drawing) {
                if (special) slowSpeed else normalSpeed
            } else {
                if (special) fastSpeed else normalSpeed
            }
            parent.showSpeed(speed)
            if (pos.drawing) {
                //
                val realSpeed = pos.clampSpeed(direction, speed)
                if (pos.onEdge()) {
                    if (parent.numLegs() > 1) {
                        // we're on the edge
                        parent.addLeg(pos.xy())
                        pos.endDraw()
                        parent.showDrawMode(false)
                        parent.mkArea()
                    }
                    else {
                        // false start
                        parent.clearLines()
                    }
                } else {
                    // we're making a turn
                    parent.addLeg(pos.xy())
                    if (!pos.updateDrawXY(direction, realSpeed)) {
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
                if (drawKey && pos.isOnGrid(direction)) {
                    // start drawing
                    pos.startDraw()
                    parent.showDrawMode(true)
                    parent.addLeg(pos.xy())
                    pos.updateDrawXY(direction, speed)
                } else {
                    pos.updatePos(speed, direction)
                }
            }
        }
    }

    inner class JoystickData {
        val joystick = Joystick()
        var jsDir: Direction? = null
        var draw = false
        var special = false

        suspend fun runJoystick(): Unit = coroutineScope {
            var xBlocked = false
            var yBlocked = false

            launch {
                while (true) {
                    var jsEvent: JoystickEvent? = withTimeoutOrNull(10000L) { joystick.joystickChannel.receive() }
                    if (jsEvent == null) {
                        println("timeout ${jsDir} ${xBlocked} ${yBlocked}")
                        jsDir = null
                        xBlocked = false
                        yBlocked = false
                        continue
                    }
                    when (jsEvent.name) {
                        "rx" -> {
                            if (xBlocked)
                                continue
                            if (jsEvent.value == 0.0f) {
                                println("rx: 0 ${jsDir} ${xBlocked} ${yBlocked}")
                                yBlocked = false
                                jsDir = null
                            } else if (!xBlocked) {
                                yBlocked = true
                                if (jsEvent.value < 0.0) {
                                    jsDir = Direction.LEFT
                                } else {
                                    jsDir = Direction.RIGHT
                                }
                            }
                        }

                        "ry" -> {
                            if (yBlocked)
                                continue
                            if (jsEvent.value == 0.0f) {
                                println("ry: 0 ${jsDir} ${xBlocked} ${yBlocked}")
                                xBlocked = false
                                jsDir = null
                            } else if (!yBlocked) {
                                xBlocked = true
                                if (jsEvent.value < 0.0) {
                                    jsDir = Direction.UP
                                } else {
                                    jsDir = Direction.DOWN
                                }
                            }
                        }

                        "z" -> {
                            if (jsEvent.value > 0.5) {
                                draw = true
                            } else if (jsEvent.value < -0.5) {
                                draw = false
                            }
                        }
                        "rz" -> {
                            if (jsEvent.value > 0.5) {
                                special = true
                            } else if (jsEvent.value < -0.5) {
                                special = false
                            }
                        }
                    }
                }
            }
        }

        suspend fun makeMoves(): Unit = coroutineScope {
            launch {
                while (true) {
                    if (jsDir != null) {
                        move(jsDir!!, special, draw)
                    }
                    delay(20)
                }
            }
        }
    }

    suspend fun draw(g: Graphics2D) {
        pos.mutex.withLock {
            // show player
            g.color = Color.RED
            val (x, y) = pos.xy()
            g.fillRect(x - 10, y - 10, 20, 20)
            // show position
            parent.showLoc(pos.posStr)
        }
    }
}


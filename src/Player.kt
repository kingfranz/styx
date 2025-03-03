import ArenaMask.MaskType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import kotlin.math.abs
import kotlin.math.hypot

///////////////////////////////////////////////////////////////////////////////

class Player(val parent: iArena) {

    enum class Direction { UP, RIGHT, DOWN, LEFT }

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

        val xy: Point
            get() {
                return position
            }

        val dir: Direction
            get() {
                return drawDirection
            }

        fun onEdge(): Boolean {
            return parent.isOnEdge(position)
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
            position = position.translate(delta)
        }

        fun updateDrawXY(delta: Point): Boolean {
            drawDirection = delta.direction()
            position = position.translate(delta)
            return !onEdge()
        }
    }

    var slowSpeed: Int = 3
    var normalSpeed: Int = 5
    var fastSpeed: Int = 10
    val pos = PlayerPos(parent)
    val joystick = Joystick()

    suspend fun reset() {
        pos.reset()
    }

    fun inverseDir(dir: Direction): Direction {
        return when(dir) {
            Direction.UP -> Direction.DOWN
            Direction.DOWN -> Direction.UP
            Direction.RIGHT -> Direction.LEFT
            Direction.LEFT -> Direction.RIGHT
        }
    }

    fun validateMove(delta: Point, forDrawing: Boolean): Point {
        if(forDrawing) {
            return parent.arenaMask.validDraw(pos.xy, delta)
        }
        else {
            return parent.arenaMask.validMove(pos.xy, delta)
        }
    }

    fun  clampLeg(lastWp: Vec, prevWp: Vec, speedDir: Vec): Vec {
        val prevVector = (lastWp - prevWp).normalize()
        val lastVector = ((pos.xy + speedDir) - lastWp).normalize()
        if (prevVector.x != 0.0 && prevVector.y != 0.0) {
            throw Exception("clampLeg")
        }
        val prevDir = prevVector.direction()
        if (abs(lastVector.y) > abs(lastVector.x)) {
            // vertical
            val speedDir2 = Vec(0, speedDir.y)
            if (inverseDir(prevDir) == lastVector.direction()) {
                // no reverse
                return Vec(0,0)
            }
              return speedDir2
        }
        else {
            // horizontal
            val speedDir2 = Vec(speedDir.x, 0)
            if (inverseDir(prevDir) == lastVector.direction()) {
                // no reverse
                return Vec(0,0)
            }
            return speedDir2
        }
    }

    fun backOnEdge(speed: Vec) {
        pos.endDraw()
        parent.showDrawMode(false)
        if (parent.numLegs() > 1) {
            // we're on the edge
            parent.addLeg(pos.xy, speed.direction())
            parent.mkArea()
        } else {
            // false start
            parent.clearLines()
        }
    }

    fun firstDraw(speed: Vec) {
        if (!pos.updateDrawXY(speed)) {
            // we're back on the edge
            parent.addLeg(pos.xy, speed.direction())
            pos.endDraw()
            parent.showDrawMode(false)
            parent.mkArea()
        }
    }

    suspend fun move(_speed: Vec, drawKey: Boolean) {
        var speed = _speed
        pos.mutex.withLock {
            if (pos.drawing) {
                if (pos.onEdge()) {
                    backOnEdge(speed)
                } else if (parent.numLegs() == 0) {
                    firstDraw(speed)
                } else {
                    val (lastPoint, lastDir) = parent.getLeg(parent.numLegs() - 1)
                    if (speed.direction() == inverseDir(lastDir)) {
                        // reverse
                        return
                    }
                    if (speed.direction() == lastDir) {
                        // same direction
                        pos.updateDrawXY(speed)
                        return
                    }
                    // check minimum length before turning
                    if (lastPoint.distance(pos.xy) >= 30) {
                        // long enough
                        parent.addLeg(pos.xy, speed.direction())
                        pos.updateDrawXY(speed)
                    } else {
                        // not long enough
                        return
                    }
                }
            } else {
                // just moving along the edge
                parent.showDrawMode(false)
                if (drawKey && speed != Point(0, 0)) {
                    // start drawing
                    pos.startDraw()
                    parent.showDrawMode(true)
                    parent.addLeg(pos.xy, speed.direction())
                    pos.updateDrawXY(speed)
                } else if(speed != Point(0, 0)) {
                    pos.updatePos(speed)
                }
            }
        }
    }

    suspend fun kbdMove(dir: Direction, slow: Boolean, draw: Boolean) {
        val speed = if (pos.drawing) {
            if (slow) slowSpeed else normalSpeed
        } else {
            if (slow) fastSpeed else normalSpeed
        }
        val delta = mkVec(dir, speed)
        val realSpeed = validateMove(delta, pos.drawing)
        move(realSpeed, draw)
    }

    suspend fun makeMoves(): Unit = coroutineScope {
        try {
            launch {
                while (true) {
                    try {
                        joystick.mutex.withLock {
                            if (joystick.jsDir != null) {
                                val speed = if (pos.drawing) {
                                    if (joystick.special) slowSpeed else normalSpeed
                                } else {
                                    if (joystick.special) fastSpeed else normalSpeed
                                }
                                parent.showSpeed(speed)
                                val delta = mkVec(joystick.jsDir!!, speed)
                                val realSpeed = validateMove(delta, pos.drawing || joystick.draw)
                                move(realSpeed, joystick.draw)
                            }
                        }
                        delay(20)
                    }
                    catch (e: Exception) {
                        println("makeMoves2: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
        catch (e: Exception) {
            println("makeMoves: ${e.message}")
            e.printStackTrace()
        }
        finally {
            println("makeMoves: finally")
        }
    }

    suspend fun draw(g: Graphics2D) {
        pos.mutex.withLock {
            // show player
            g.color = Color.RED
            g.fillRect(pos.xy.x - 10, pos.xy.y - 10, 20, 20)
            // show position
            parent.showLoc(pos.posStr)
        }
    }
}


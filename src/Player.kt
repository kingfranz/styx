package Styx

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point

///////////////////////////////////////////////////////////////////////////////

class Player(val parent: iArena) {

    // TODO add levels and score and 80% win

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
        if(parent.weLost())
            return Point(0, 0)
        if(forDrawing) {
            return parent.arenaMask.validDraw(pos.xy, delta)
        }
        else {
            return parent.arenaMask.validMove(pos.xy, delta)
        }
    }

    fun backOnEdge(speed: Vec) {
        pos.endDraw()
        parent.showDrawMode(false)
        if (parent.numLegs() > 0) {
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
                } else {
                    val (lastPoint, lastDir) = parent.getLeg(parent.numLegs() - 1)
                    if (speed.direction() == inverseDir(lastDir)) {
                        // reverse
                        return
                    }
                    if (speed.direction() == lastDir) {
                        // same direction
                        pos.updateDrawXY(speed)
                        parent.addLeg(pos.xy, speed.direction())
                        return
                    }
                    // check minimum length before turning
                    if (parent.countStraight() >= 30) {
                        // long enough
                        pos.updateDrawXY(speed)
                        parent.addLeg(pos.xy, speed.direction())
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
                    // add start position
                    parent.addLeg(pos.xy, speed.direction())
                    // move to new position
                    pos.updateDrawXY(speed)
                    // save new position
                    parent.addLeg(pos.xy, speed.direction())
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
                                parent.showLvl(speed) // TODO move this
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

    var radius = 10
    var down = true

    suspend fun draw(g: Graphics2D) {
        pos.mutex.withLock {
            // show player
            g.color = Color.RED
            drawCircle(g, pos.xy.x, pos.xy.y, radius)
            if(down && radius > 2) {
                radius--
            }
            else if(down && radius <= 2) {
                down = false
                radius++
            }
            else if(!down && radius < 10) {
                radius++
            }
            else if(!down && radius >= 10) {
                down = true
                radius--
            }
            // show position
            parent.showLoc(pos.posStr)
        }
    }
}


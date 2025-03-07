package Styx

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.util.LinkedList

///////////////////////////////////////////////////////////////////////////////

class Sprite(val parent: Arena, val edgeSz: Int) {
    data class SpriteSegment(
        val line: Segment,
        val color: Color = Color.BLACK
    )

    val spriteLength = 10 // number of segments
    val spriteSize = 250 // segment length
    val stepSize = 5 // pixels
    var spriteSpeed: Long = 20 // ms delay
    val maxAngle = Math.toRadians(30.0) // max angle change
    val tail = LinkedList<SpriteSegment>() // tail segments
    val tailMutex = Mutex(false) //
    var currentPoint = Point(500, 500) // middle of the lead segment
    var currentAngle = 0.0 // angle of the lead segment

    suspend fun reset() {
        tailMutex.withLock {
            tail.clear()
        }
        currentPoint = Point(500, 500)
        currentAngle = 0.0
    }

    fun newAngle(a: Double): Double {
        return (a + Math.random() * 2 * maxAngle - maxAngle) % (2 * Math.PI)
    }

    fun newPoint(p: Point, a: Double, d: Int): Point {
        val x = p.x + (Math.cos(a) * d).toInt()
        val y = p.y + (Math.sin(a) * d).toInt()
        return Point(x, y)
    }

    fun mkSegment(p: Point, a: Double, d: Int, clr: Color): SpriteSegment {
        val p1 = newPoint(p, a + Math.PI / 4, d / 2)
        val p2 = newPoint(p, a - Math.PI / 4, d / 2)
        return SpriteSegment(Segment(Point(p1.x, p1.y), Point(p2.x, p2.y)), clr)
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
        tailMutex.withLock {
            tail.addFirst(mkSegment(currentPoint,
                currentAngle,
                spriteSize,
                ColorCycle.rndClr()))
            if (tail.size > spriteLength) {
                tail.removeLast()
            }
        }
    }

    fun getEq(p1: Point, p2: Point): Pair<Double, Double> {
        val a = (p2.y - p1.y).toDouble() / (p2.x - p1.x).toDouble()
        val b = p1.y - a * p1.x
        return Pair(a, b)
    }

    val minX get() = 0
    val maxX get() = parent.width - 2 * edgeSz
    val minY get() = 0
    val maxY get() = parent.height - 2 * edgeSz

    enum class HitObj {
        NONE, WALL, AREA, LINE
    }

    data class Segment(val p1: Point, val p2: Point)

    data class LineHit(
        val hitSegment: Int,
        val segment: Segment,
        val hitPoint: Point,
        val hitObj: HitObj,
        val hitObject: Segment,
        val spritePos: Point,
        val spriteAngle: Double
    )

    fun collisionDetect(): LineHit? {
        var tailNum = 0
        for(segment in tail) {
            val p1 = segment.line.p1
            val p2 = segment.line.p2
            val collition = parent.arenaMask.fakeDraw(p1, p2) { p ->
                if(tailNum == 0) {
                    // only check lead segment against walls
                    if (p.x < 0 || p.x >= parent.arenaSz || p.y < 0 || p.y >= parent.arenaSz) {
                        val target =
                            if (p.x < 0)
                                Segment(Point(0, 0), Point(0, 999))
                            else if (p.x > 999)
                                Segment(Point(999, 0), Point(999, 999))
                            else if (p.y < 0)
                                Segment(Point(0, 0), Point(999, 0))
                            else if (p.y > 999)
                                Segment(Point(0, 999), Point(999, 999))
                            else
                                Segment(Point(0, 0), Point(999, 999))
                        return@fakeDraw LineHit(
                            0,
                            segment.line,
                            p,
                            HitObj.WALL,
                            target,
                            currentPoint,
                            currentAngle
                        )
                    }
                    val mt = parent.arenaMask.get(p)
                    if (mt == ArenaMask.MaskType.WALL_CLR) {
                        return@fakeDraw LineHit(
                            0,
                            segment.line,
                            p,
                            HitObj.WALL,
                            parent.arenaMask.getWall(p, false)!!,
                            currentPoint,
                            currentAngle)
                    }
                }
                // check all segemnts against LINE_CLR
                if (p.x >= 0 && p.x < parent.arenaSz && p.y >= 0 && p.y < parent.arenaSz) {
                    val mt = parent.arenaMask.get(p)
                    if (mt == ArenaMask.MaskType.LINE_CLR) {
                        return@fakeDraw LineHit(
                            0,
                            segment.line,
                            p,
                            HitObj.LINE,
                            parent.arenaMask.getWall(p, true)!!,
                            currentPoint,
                            currentAngle
                        )
                    }
                }
                return@fakeDraw null
            }
            if (collition != null) {
                return collition
            }
            tailNum++
        }
        return null
    }

    fun epsilon(): Double {
        val slump = Math.random() * 2.0 - 1.0
        return Math.toRadians(slump)
    }

    suspend fun step() {
        val ret = collisionDetect()
        if (ret == null) {
            currentAngle = normalizeAngle(newAngle(currentAngle))
        } else if(ret.hitObj == HitObj.LINE) {
            parent.showHit(ret.hitPoint)
            return
        }
        else {
            currentAngle = calculateReflectionAngle(ret) + epsilon()
        }
        tailMutex.withLock {
            currentPoint = newPoint(currentPoint, currentAngle, stepSize)
        }
        store()
    }

    suspend fun run(): Unit = coroutineScope {
        try {
            while (true) {
                step()
                delay(spriteSpeed)
            }
        } catch (e: Exception) {
            println("run: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun draw(g: Graphics2D) {
        g.stroke = java.awt.BasicStroke(4.0f)
        tailMutex.withLock {
            for (segment in tail) {
                g.color = segment.color
                g.drawLine(segment.line.p1.x, segment.line.p1.y, segment.line.p2.x, segment.line.p2.y)
            }
        }
    }

    fun calculateReflectionAngle(hit: LineHit): Double {
        val epsilon = Math.random() * 5.0 - 2.5
        if (hit.hitObject.p1.x == hit.hitObject.p2.x) {
            // vertical line
            when (hit.spriteAngle) {
                in 0.0..Math.PI -> return normalizeAngle(Math.PI - hit.spriteAngle + epsilon)
                else -> return normalizeAngle(3 * Math.PI - hit.spriteAngle + epsilon)
            }
        } else if (hit.hitObject.p1.y == hit.hitObject.p2.y) {
            // horizontal line
            when (hit.spriteAngle) {
                in Math.PI / 2..3 * Math.PI / 2 -> return normalizeAngle(2 * Math.PI - hit.spriteAngle + epsilon)
                else -> return normalizeAngle(Math.PI - hit.spriteAngle + epsilon)
            }
        } else {
            val (a, b) = getEq(hit.hitObject.p1, hit.hitObject.p2)
            val angle = Math.atan(a)
            val newAngle = if (hit.spriteAngle < Math.PI) {
                if (a > 0) {
                    normalizeAngle(Math.PI - hit.spriteAngle + 2 * angle + epsilon)
                } else {
                    normalizeAngle(Math.PI - hit.spriteAngle + angle + epsilon)
                }
            } else {
                if (a > 0) {
                    normalizeAngle(3 * Math.PI - hit.spriteAngle + angle + epsilon)
                } else {
                    normalizeAngle(3 * Math.PI - hit.spriteAngle + 2 * angle + epsilon)
                }
            }
            return newAngle
        }
    }
}


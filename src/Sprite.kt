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
        tailMutex.withLock {
            tail.addFirst(mkSegment(currentPoint, currentAngle, spriteSize, rndClr()))
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

//    fun collisionDetect2(): LineHit? {
//        // first check the perimeter
//        if (tail.size > 0) {
//            val s1 = Point(tail.first().x1, tail.first().y1)
//            val s2 = Point(tail.first().x2, tail.first().y2)
//            //-------------------------------------------
//            // top edge
//            //-------------------------------------------
//            var e1 = Point(minX, minY)
//            var e2 = Point(maxX, minY)
//            var hit = myIntersect(Pair(s1, s2), Pair(e1, e2), HitObj.WALL)
//            if (hit != null) {
//                //println("Hit.top at $hit")
//                return LineHit(
//                    0,
//                    s1, s2,
//                    hit.hitPoint,
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//            if (tail.first().y1 < minY || tail.first().y2 < minY) {
//                return LineHit(
//                    0,
//                    s1, s2,
//                    Point(tail.first().x1, minY),
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//            //-------------------------------------------
//            // right edge
//            //-------------------------------------------
//            e1 = Point(maxX, minY)
//            e2 = Point(maxX, maxY)
//            hit = myIntersect(Pair(s1, s2), Pair(e1, e2), HitObj.WALL)
//            if (hit != null) {
//                //println("Hit.right at $hit")
//                return LineHit(
//                    0,
//                    s1, s2,
//                    hit.hitPoint,
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//            if (tail.first().x1 > maxX || tail.first().x2 > maxX) {
//                return LineHit(
//                    0,
//                    s1, s2,
//                    Point(maxX, tail.first().y1),
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//            //-------------------------------------------
//            // bottom edge
//            //-------------------------------------------
//            e1 = Point(maxX, maxY)
//            e2 = Point(minX, maxY)
//            hit = myIntersect(Pair(s1, s2), Pair(e1, e2), HitObj.WALL)
//            if (hit != null) {
//                //println("Hit.bottom at $hit")
//                return LineHit(
//                    0,
//                    s1, s2,
//                    hit.hitPoint,
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//            if (tail.first().y1 > maxY || tail.first().y2 > maxY) {
//                return LineHit(
//                    0,
//                    s1, s2,
//                    Point(tail.first().x1, maxY),
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//            //-------------------------------------------
//            // left edge
//            //-------------------------------------------
//            e1 = Point(minX, maxY)
//            e2 = Point(minX, minY)
//            hit = myIntersect(Pair(s1, s2), Pair(e1, e2), HitObj.WALL)
//            if (hit != null) {
//                //println("Hit.left at $hit")
//                return LineHit(
//                    0,
//                    s1, s2,
//                    hit.hitPoint,
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//            if (tail.first().x1 < minX || tail.first().x2 < minX) {
//                return LineHit(
//                    0,
//                    s1, s2,
//                    Point(minX, tail.first().y1),
//                    HitObj.WALL,
//                    e1, e2,
//                    currentPoint,
//                    currentAngle
//                )
//            }
//        }
//
//        // then check the drawn areas
//        val ret = intersectsAnyArea()
//        if (ret != null) {
//            //println("Hit.boxes (${ret.segPoint1.x}, ${ret.segPoint2.y}) ${ret.hitPoint.x}, ${ret.hitPoint.y}")
//            return ret
//        }
//
//        // and then the current lines
//        val (num, xs, ys) = parent.getLines()
//        if (num > 1) {
//            for (i in 1 until num) {
//                val s1 = Point(xs[i - 1], ys[i - 1])
//                val s2 = Point(xs[i], ys[i])
//                for (segment in tail) {
//                    val hit = myIntersect(
//                        Pair(s1, s2),
//                        Pair(Point(segment.x1, segment.y1),
//                            Point(segment.x2, segment.y2)),
//                        HitObj.LINE)
//                    if (hit != null) {
//                        //println("Hit.lines at $hit")
//                        return hit
//                    }
//                }
//            }
//        }
//        return null
//    }

    suspend fun step() {
        val ret = collisionDetect()
        if (ret == null) {
            currentAngle = normalizeAngle(newAngle(currentAngle))
        } else if(ret.hitObj == HitObj.LINE) {
            parent.showHit(ret.hitPoint)
        }
        else {
            currentAngle = calculateReflectionAngle(ret)
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
        g.stroke = java.awt.BasicStroke(2.0f)
        tailMutex.withLock {
            for (segment in tail) {
                g.color = segment.color
                g.drawLine(segment.line.p1.x, segment.line.p1.y, segment.line.p2.x, segment.line.p2.y)
            }
        }
    }
/*

    fun intersectsAnyArea(): LineHit? {
        if(tail.size > 0) {
            val segment = tail.first()
            for (polygon in parent.areas) {
                if (polygon.contains(segment.line.p1.x, segment.line.p1.y) ||
                    polygon.contains(segment.line.p2.x, segment.line.p2.y)) {
                    val s1 = segment.line.p1
                    val s2 = segment.line.p2
                    return LineHit(
                        0,
                        s1, s2,
                        Point(0, 0),
                        HitObj.AREA,
                        Point(0, 0), Point(0, 0),
                        currentPoint,
                        currentAngle
                    )
                }
            }
        }
        return null
    }

    fun myIntersect(wall: Pair<Point, Point>, segment: Pair<Point, Point>, objType: HitObj): LineHit? {
        if (wall.first.x == wall.second.x) {
            // vertical wall
            if (segment.first.x == segment.second.x) {
                // vertical segment
                if (wall.first.x == segment.first.x) {
                    return LineHit(
                        0,
                        segment.first, segment.second,
                        segment.first,
                        objType,
                        wall.first, wall.second,
                        currentPoint,
                        currentAngle
                    )
                }
                else {
                    return null
                }
            } else if(segment.first.x < wall.first.x && segment.second.x < wall.first.x) {
                return null
            } else if(segment.first.x > wall.first.x && segment.second.x > wall.first.x) {
                return null
            }
            else {
                val (a, b) = getEq(segment.first, segment.second)
                val y = a * wall.first.x + b
                if (y >= wall.first.y && y <= wall.second.y) {
                    return LineHit(
                        0,
                        segment.first, segment.second,
                        Point(wall.first.x, y.toInt()),
                        objType,
                        wall.first, wall.second,
                        currentPoint,
                        currentAngle
                    )
                }
                else {
                    return null
                }
            }
        } else if (wall.first.y == wall.second.y) {
            // horizontal wall
            if (segment.first.y == segment.second.y) {
                // horizontal segment
                if (wall.first.y == segment.first.y) {
                    return LineHit(
                        0,
                        segment.first, segment.second,
                        segment.first,
                        objType,
                        wall.first, wall.second,
                        currentPoint,
                        currentAngle
                    )
                }
                else {
                    return null
                }
            } else if (segment.first.y < wall.first.y && segment.second.y < wall.first.y) {
                return null
            } else if (segment.first.y > wall.first.y && segment.second.y > wall.first.y) {
                return null
            } else {
                val (a, b) = getEq(segment.first, segment.second)
                val x = (wall.first.y - b) / a
                if (x >= wall.first.x && x <= wall.second.x) {
                    return LineHit(
                        0,
                        segment.first, segment.second,
                        Point(x.toInt(), wall.first.y),
                        objType,
                        wall.first, wall.second,
                        currentPoint,
                        currentAngle
                    )
                }
                else {
                    return null
                }
            }
        } else {
            val (a1, b1) = getEq(wall.first, wall.second)
            val (a2, b2) = getEq(segment.first, segment.second)
            val x = (b2 - b1) / (a1 - a2)
            if (x >= wall.first.x && x <= wall.second.x) {
                val y = a1 * x + b1
                if (y >= wall.first.y && y <= wall.second.y) {
                    return LineHit(
                        0,
                        segment.first, segment.second,
                        Point(x.toInt(), y.toInt()),
                        objType,
                        wall.first, wall.second,
                        currentPoint,
                        currentAngle
                    )
                }
                else {
                    return null
                }
            }
            else {
                return null
            }
        }
        return null
    }
*/

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


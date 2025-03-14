package Styx

import Styx.ArenaMask.MaskType
import Styx.minus
import Styx.times
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Polygon
import java.util.LinkedList
import kotlin.collections.get
import kotlin.collections.plusAssign
import kotlin.collections.removeLast
import kotlin.compareTo
import kotlin.inc
import kotlin.text.clear
import kotlin.text.get
import kotlin.text.set

class DrawnLines {
    class Line(val p1: Point,
               val p2: Point,
               val slow: Boolean) {

        fun eq(): Pair<Double, Double> { // (k, m)
            if(p1.x == p2.x)
                return Pair(Double.POSITIVE_INFINITY, p1.x.toDouble())
            val a = (p2.y - p1.y).toDouble() / (p2.x - p1.x).toDouble()
            val b = p1.y - a * p1.x
            return Pair(a, b)
        }

        fun length(): Double {
            return p1.distance(p2).toDouble()
        }

        fun direction(): Player.Direction {
            if(p1.x == p2.x) {
                return if(p1.y < p2.y) Player.Direction.DOWN else Player.Direction.UP
            }
            return if(p1.x < p2.x) Player.Direction.RIGHT else Player.Direction.LEFT
        }

        fun draw(g: Graphics2D) {
            g.stroke = java.awt.BasicStroke(3.0f)
            g.color = if(slow) Color.GREEN else Color.BLACK
            g.drawLine(p1.x, p1.y, p2.x, p2.y)
        }
    }

    /////////////////////////////////////////////////

    val lines = ArrayList<Line>() // (x, y)
    var savedPos: Point? = null
    val mutex = Mutex(false)

    /////////////////////////////////////////////////

    suspend fun reset() {
        mutex.withLock {
            lines.clear()
            savedPos = null
        }
    }

    //------------------------------------------

    suspend fun add(p1: Point, p2: Point, isSlow:  Boolean) {
        if(p1 == p2) {
            return
        }
        mutex.withLock {
            if (lines.size > 0 && lines.last().p2 != p1) {
                throw Exception("Line not connected")
            }
            lines.add(Line(p1, p2, isSlow))
        }
    }

    //------------------------------------------

    suspend fun add(wp: Point, isSlow:  Boolean) {
        mutex.withLock {
            if (savedPos != null) {
                lines.add(Line(savedPos!!, wp, isSlow))
            }
            savedPos = wp
        }
    }

    //------------------------------------------

    suspend fun getStartStop(): Pair<Point, Point> {
        mutex.withLock {
            if (lines.size < 1) {
                throw Exception("No lines")
            }
            return Pair(lines[0].p1, lines[lines.size - 1].p2)
        }
    }

    //------------------------------------------

    operator fun get(i: Int): Line {
        if(i < 0)
            return lines[lines.size + i]
        return lines[i]
    }

    //------------------------------------------

    val size: Int
        get() {
        return lines.size
    }

    //------------------------------------------

    suspend fun countStraight(): Int {
        mutex.withLock {
            val targetDir = lines.last().direction()
            var dist = 0.0
            lines.reversed().takeWhile { it.direction() == targetDir }.forEach {
                dist += it.length()
            }
            return dist.toInt()
        }
    }

    //------------------------------------------

    suspend fun draw(g: Graphics2D) {
        g.stroke = java.awt.BasicStroke(3.0f)
        mutex.withLock {
            lines.forEach { it.draw(g) }
        }
    }
}
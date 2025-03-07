package Styx

import Styx.Player.Direction
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Polygon
import java.awt.geom.Point2D
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min


typealias Vec = Point

fun Int.toMaskType(): ArenaMask.MaskType {
    val tmp: Byte = this.toByte()
    return when (tmp) {
        ArenaMask.MaskType.EMPTY_CLR.value -> ArenaMask.MaskType.EMPTY_CLR
        ArenaMask.MaskType.WALL_CLR.value -> ArenaMask.MaskType.WALL_CLR
        ArenaMask.MaskType.AREA_CLR.value -> ArenaMask.MaskType.AREA_CLR
        ArenaMask.MaskType.LINE_CLR.value -> ArenaMask.MaskType.LINE_CLR
        ArenaMask.MaskType.ERROR_CLR.value -> ArenaMask.MaskType.ERROR_CLR
        else -> ArenaMask.MaskType.EMPTY_CLR
    }
}

fun clamp(value: Int, min: Int, max: Int): Int {
    return max(min.toDouble(), min(max.toDouble(), value.toDouble())).toInt()
}

fun Point.translate(p: Point): Point {
    return Point(x + p.x, y + p.y)
}

operator fun Point.minus(p: Point): Point {
    return Point(x - p.x, y - p.y)
}

fun Point.magnitude(): Double {
    return hypot(x.toDouble(), y.toDouble())
}

fun Point.normalize(): Point2D.Double {
    val mag = magnitude()
    return Point2D.Double(x / mag, y / mag)
}

fun Point.direction(): Direction {
    if (abs(x) > abs(y)) {
        if (x < 0) return Direction.LEFT else return Direction.RIGHT
    }
    if (y < 0) return Direction.UP else return Direction.DOWN
}

fun Point2D.Double.direction(): Direction {
    if (abs(x) > abs(y)) {
        if (x < 0) return Direction.LEFT else return Direction.RIGHT
    }
    if (y < 0) return Direction.UP else return Direction.DOWN
}

operator fun Point.plus(p: Point): Point {
    return Point(x + p.x, y + p.y)
}

operator fun Point.times(f: Int): Point {
    return Point(x * f, y * f)
}

operator fun Point2D.Double.times(f: Double): Point2D.Double {
    return Point2D.Double(x * f, y * f)
}

fun Point2D.Double.toPoint(): Point {
    return Point(x.toInt(), y.toInt())
}

fun Point.iter(): List<Point> {
    val n = normalize()
    val m = magnitude().toInt()
    return (1..m).map { (n * it.toDouble()).toPoint() }
}

fun mkVec(direction: Direction, dist: Int): Point {
    return when (direction) {
        Direction.UP -> Point(0, -dist)
        Direction.DOWN -> Point(0, dist)
        Direction.RIGHT -> Point(dist, 0)
        Direction.LEFT -> Point(-dist, 0)
    }
}

fun drawCircle(g: Graphics2D, x: Int, y: Int, r: Int) {
    g.fillOval(x - r, y - r, 2 * r, 2 * r)
}

fun Polygon.str(): String {
    val sb = StringBuilder()
    for (i in 0 until npoints) {
        sb.append("(${xpoints[i]}, ${ypoints[i]}) ")
    }
    return sb.toString()
}

fun Polygon(ps: List<Point>): Polygon {
    val xs = IntArray(ps.size) { i -> ps[i].x }
    val ys = IntArray(ps.size) { i -> ps[i].y }
    return Polygon(xs, ys, ps.size)
}

fun List<Point>.str(): String {
    val sb = StringBuilder()
    for (i in 0 until size) {
        sb.append("(${get(i).x}, ${get(i).y}) ")
    }
    return sb.toString()
}

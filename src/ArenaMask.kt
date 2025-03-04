import java.awt.BasicStroke
import java.awt.Color
import java.awt.Point
import java.awt.Polygon
import java.awt.Stroke
import java.util.SortedSet
import kotlin.math.max
import kotlin.math.min

class ArenaMask(val width: Int, val height: Int) {

    var _color: Color = Color(0, 0, 0)
    var color: Color
        get() = _color
        set(value) { _color = value }

    var _stroke: Stroke = BasicStroke(1.0f)
    var stroke: Stroke
        get() = _stroke
        set(value) { _stroke = value }

    enum class MaskType(val value: Byte) {
        EMPTY_CLR(0),
        WALL_CLR(10),
        AREA_CLR(20),
        LINE_CLR(24),
        ERROR_CLR(25)
    }

    val mask = Array<MaskType>(width * height) { MaskType.EMPTY_CLR }

    fun reset() {
        for (i in 0 until width * height) {
            mask[i] = MaskType.EMPTY_CLR
        }
    }

    fun get(x: Int, y: Int): MaskType {
        return mask[y * width + x]
    }

    fun get(p: Point): MaskType {
        return get(p.x, p.y)
    }

    fun set(x: Int, y: Int, value: MaskType) {
        mask[y * width + x] = value
    }

    fun set(p: Point, value: MaskType) {
        set(p.x, p.y, value)
    }

    fun validPoint(p: Point): Boolean {
        return p.x >= 0 && p.x < width && p.y >= 0 && p.y < height
    }

    fun validPoint(x: Int, y: Int): Boolean {
        return x >= 0 && x < width && y >= 0 && y < height
    }

    fun validMove(start: Point, delta: Vec): Vec {
        // only WALL_CLR
        if (!validPoint(start))
            return Vec(0, 0)
        val chain = delta
            .iter()
            .map {
                val pp = start.translate(it)
                Pair(it, validPoint(pp) && get(pp) == MaskType.WALL_CLR)
            }
            .takeWhile { it.second }
        if (chain.isEmpty())
            return Vec(0, 0)
        return chain.last().first
    }

    fun validDraw(start: Point, delta: Vec): Vec {
        // only EMPTY but can end on WALL
        if (!validPoint(start))
            return Vec(0, 0)
        // also check for xover
        val steps = delta
            .iter()
            .map {
                val pp = start.translate(it)
                Triple(it, pp, validPoint(pp) && (get(pp) == MaskType.EMPTY_CLR || get(pp) == MaskType.WALL_CLR)) }
        var lastEmpty = -1
        var firstWall = -1
        for(i in 0 .. steps.size-1) {
            if (steps[i].third && get(steps[i].second) == MaskType.EMPTY_CLR)
                lastEmpty = i
            if (steps[i].third && get(steps[i].second) == MaskType.WALL_CLR && firstWall == -1)
                firstWall = i
        }
        if (lastEmpty == -1) {
            if (firstWall != 0)
                return Vec(0, 0)
            return steps[0].first
        }
        if (firstWall == -1) {
            return steps[lastEmpty].first
        }
        if (firstWall != lastEmpty+1)
            return Vec(0, 0)
        return steps[firstWall].first
    }

    fun fillRect(x: Int, y: Int, w: Int, h: Int, color: MaskType) {
        for (yy in y until y + h) {
            drawHLine(x, x+w-1, yy, color)
        }
    }

    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: MaskType) {
        drawHLine(x, x + w - 1, y, color)
        drawHLine(x, x + w - 1, y + h - 1, color)
        drawVLine(x, y, y + h - 1, color)
        drawVLine(x + w - 1, y, y + h - 1, color)
    }

    fun drawHLine(x1: Int, x2: Int, y: Int, color: MaskType) {
        val xx1 = max(0, min(x1, x2))
        val xx2 = min(max(x1, x2), width - 1)
        for (x in xx1..xx2) {
            set(x, y, color)
        }
    }

    fun drawVLine(x: Int, y1: Int, y2: Int, color: MaskType) {
        val yy1 = max(0, min(y1, y2))
        val yy2 = min(max(y1, y2), height - 1)
        for (y in yy1..yy2) {
            set(x, y, color)
        }
    }

    fun drawLine(start: Point, end: Point, color: MaskType) {
        if (start.x == end.x) {
            drawVLine(start.x, start.y, end.y, color)
            return
        }
        if (start.y == end.y) {
            drawHLine(start.x, end.x, start.y, color)
            return
        }
        val dx = Math.abs(end.x - start.x)
        val dy = Math.abs(end.y - start.y)
        val sx = if (start.x < end.x) 1 else -1
        val sy = if (start.y < end.y) 1 else -1
        var x = start.x
        var y = start.y
        var err = dx - dy
        while (true) {
            set(x, y, color)
            if (x == end.x && y == end.y) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }

    fun drawPoly(points: Polygon, color: MaskType) {
        val polyX = points.xpoints
        val polyY = points.ypoints
        val polyCorners = points.npoints
        for(i in 0 until polyCorners-1) {
            when {
                polyX[i]==polyX[i+1] -> drawVLine(polyX[i], polyY[i], polyY[i+1], color)
                polyY[i]==polyY[i+1] -> drawHLine(polyX[i], polyX[i+1], polyY[i], color)
                else -> drawLine(Point(polyX[i], polyY[i]),
                    Point(polyX[i+1], polyY[i+1]),
                    color)
            }
        }
    }

    fun poly2Lines(poly: Polygon): List<Pair<Point, Point>> {
        val polyX = poly.xpoints
        val polyY = poly.ypoints
        val polyCorners = poly.npoints
        val lines = mutableListOf<Pair<Point, Point>>()
        for(i in 0 until polyCorners-1) {
            if (polyY[i] < polyY[i+1])
                lines.add(Pair(Point(polyX[i], polyY[i]), Point(polyX[i+1], polyY[i+1])))
            else
                lines.add(Pair(Point(polyX[i+1], polyY[i+1]), Point(polyX[i], polyY[i])))
        }
        if (lines.size < 2)
            throw Exception("Polygon has less than 2 lines")
        if (lines.first().first.y < lines.last().first.y)
            lines.add(Pair(lines.first().first, lines.last().first))
        else
            lines.add(Pair(lines.last().first, lines.first().first))
        return lines
    }

    fun fPoly(poly: Polygon) {
//        val pLines = poly2Lines(poly)
//        for (pLine in pLines) {
//            drawLine(pLine.first, pLine.second, MaskType.AREA_CLR)
//        }
        for(y in poly.bounds.y until poly.bounds.y+poly.bounds.height) {
            for(x in poly.bounds.x until poly.bounds.x+poly.bounds.width) {
                if (poly.contains(x, y)) {
                    set(x, y, MaskType.AREA_CLR)
                }
            }
        }
    }

    fun fillPolygon(poly: Polygon) {
        val MAX_POLY_CORNERS = 100
        val nodeX: SortedSet<Int> = sortedSetOf()
        val pLines = poly2Lines(poly)
        val imageTop: Int = poly.ypoints.min()
        val imageBot: Int = poly.ypoints.max()
        val imageLeft: SortedSet<Int>

        // Loop through the rows of the image.
        for (pixelY in imageTop until imageBot+1) {
            // Build a list of nodes.
            nodeX.clear()
            for (pLine in pLines) {
                if (pixelY >= pLine.first.y && pixelY <= pLine.second.y) {
                    if(pLine.first.y == pLine.second.y) {
                        nodeX.add(pLine.first.x)
                        nodeX.add(pLine.second.x)
                        continue
                    }
                    val k = (pixelY - pLine.first.y).toDouble() /
                            (pLine.second.y - pLine.first.y) *
                            (pLine.second.x - pLine.first.x)
                    val nodeXValue = (pLine.first.x + Math.round(k)).toInt()
                    //if (nodeX.find({it==nodeXValue})!=null || nodeXValue==0)
                    //    throw Exception("Polygon nodeX value already exists: ${nodeXValue}")
                    nodeX.add(nodeXValue)
                }
            }
            if (nodeX.size % 2 != 0)
                throw Exception("Polygon nodeX size is not even: ${nodeX.size}")

            // Sort the nodes, via a simple "Bubble" sort.
            //nodeX.sort()

            nodeX.windowed(2, 1, false).forEach {
                drawHLine(it[0], it[1], pixelY, MaskType.AREA_CLR)
            }
        }
    }
}
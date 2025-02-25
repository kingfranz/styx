import java.awt.Point
import java.util.LinkedList

open class Wall(var p1: Point, var p2: Point) {
    fun getPoints(): Pair<Point, Point> {
        return Pair(p1, p2)
    }

    fun contains(p: Point): Boolean {
        if (p1.x == p2.x) {
            if (p1.x == p.x) {
                if (p1.y <= p.y && p.y <= p2.y) {
                    return true
                }
            }
        } else {
            if (p1.y == p.y) {
                if (p1.x <= p.x && p.x <= p2.x) {
                    return true
                }
            }
        }
        return false
    }
}

class VerticalWall: Wall {
    constructor(x: Int, y1: Int, y2: Int): super(Point(x, y1), Point(x, y2))
}

class HorizontalWall: Wall {
    constructor(y: Int, x1: Int, x2: Int): super(Point(x1, y), Point(x2, y))
}

class Walls {
    private val walls = ArrayList<Wall>()

    fun addWall(w: Wall) {
        walls.add(w)
    }

    fun getWalls(): ArrayList<Wall> {
        return walls
    }

    fun clearWalls() {
        walls.clear()
    }

    fun findWall(p: Point): Wall? {
        return walls.find { it.contains(p) }
    }

    fun addWalls(w: Walls) {
        val start = w.getWalls().first().getPoints().first
        val end = w.getWalls().last().getPoints().second
        var firstLine: Int? = null
        var lastLine: Int? = null
        walls.forEachIndexed { index, wall ->
            if(wall.contains(start)) {
                firstLine = index
            }
            if(wall.contains(end)) {
                lastLine = index
            }
        }
        if(firstLine == null || lastLine == null) {
            throw Exception("addWalls: firstLine or lastLine is null")
        }
        if(firstLine == lastLine) {
            // split it
            w.walls.add(Wall(w.walls.last().p2, walls[firstLine].p2))
            walls[firstLine].p2 = w.walls.first().p1
            walls.addAll(firstLine+1, w.walls)
            return
        }
        repeat(lastLine - firstLine) {
            walls.removeAt(firstLine + 1)
        }
        walls[firstLine].p2 = w.walls.first().p1
        walls[lastLine].p1 = w.walls.last().p2
        walls.addAll(firstLine+1, w.walls)
    }
}
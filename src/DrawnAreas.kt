package Styx

import Styx.ArenaMask.MaskType
import Styx.DrawnLines.Line
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Polygon
import java.util.LinkedList

class DrawnAreas {

    class Area(val poly: Polygon, val slow: Boolean) {
        var cache = 0
        val surface: Int
            get() {
                if(cache == 0) {
                    cache = calcArea()
                }
                return cache
            }

        fun calcArea(): Int  {
            var sum = 0
            for (y in poly.bounds.y until poly.bounds.y + poly.bounds.height) {
                for (x in poly.bounds.x until poly.bounds.x + poly.bounds.width) {
                    if (poly.contains(x, y)) {
                        sum++
                    }
                }
            }
            return if(slow) sum * 2 else sum
        }

        fun contains(p: Point): Boolean {
            return poly.contains(p)
        }

        fun draw(g: Graphics2D) {
            g.color = Color.BLACK
            g.stroke = java.awt.BasicStroke(1.0f)
            g.drawPolygon(poly)
            g.color = if(slow) Color(255, 0, 0, 100) else Color(0, 255, 0, 100)
            g.fillPolygon(poly)
        }
    }

    val areas = ArrayList<Area>()
    private var areaCache = 0

    val totalArea: Int
        get() {
            if (areaCache == 0) {
                areaCache = areas.sumOf { it.surface }
            }
            return areaCache
        }

    operator fun get(i: Int): Area {
        if(i < 0)
            return areas[areas.size + i]
        return areas[i]
    }

    fun reset() {
        areas.clear()
        areaCache = 0
    }

     fun add(p: Polygon, slow: Boolean) {
        areas.add(Area(p, slow))
    }

    fun draw(g: Graphics2D) {
        for (a in areas) {
            a.draw(g)
        }
    }
}
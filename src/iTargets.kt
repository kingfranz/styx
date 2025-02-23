import java.awt.Point
import java.awt.Polygon
import java.util.LinkedList

interface iTargets {
    fun getLines(): Triple<Int, IntArray, IntArray>
    fun getDrawnAreas(): LinkedList<Polygon>
}
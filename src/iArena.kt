import java.awt.Point
import java.util.LinkedList
import javax.swing.JComponent

interface iArena {
    fun showDrawMode(active: Boolean): Unit
    fun showSpeed(value: Int): Unit
    fun showLoc(value: String): Unit
    fun addLeg(p: Point): Unit
    fun mkArea(): Unit
    fun clearLines(): Unit
    fun clearAreas(): Unit
    fun numLegs(): Int
    fun getWalls(): Walls
    fun isPosAvailable(p: Point): Boolean
}
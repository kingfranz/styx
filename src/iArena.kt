import java.awt.Point
import java.util.LinkedList
import javax.swing.JComponent

interface iShow {
    fun showDrawMode(active: Boolean): Unit
    fun showSpeed(value: Int): Unit
    fun showLoc(value: String): Unit
    fun showPercent(value: Int): Unit
}

interface iArena: iShow {
    val arenaMask: ArenaMask
    fun addLeg(p: Point, dir: Player.Direction): Unit
    fun getLeg(i: Int): Pair<Point, Player.Direction>
    fun mkArea(): Unit
    fun clearLines(): Unit
    fun clearAreas(): Unit
    fun numLegs(): Int
    fun isPosAvailable(p: Point): Boolean
    fun isOnEdge(p: Point): Boolean
    fun getPointType(p: Point): ArenaMask.MaskType
}
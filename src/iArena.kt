import javax.swing.JComponent

interface iArena {
    fun showDrawMode(active: Boolean): Unit
    fun showSpeed(value: Int): Unit
    fun showLoc(value: String): Unit
    fun addLeg(p: Pair<Int,Int>): Unit
    fun mkArea(): Unit
    fun clearLines(): Unit
    fun clearAreas(): Unit
    fun numLegs(): Int
}
package Styx

import java.awt.Point

interface iShow {
    fun showDrawMode(active: Boolean): Unit
    fun showLvl(value: Int): Unit
    fun showLoc(value: String): Unit
    fun showPercent(value: Int): Unit
    fun showScore(value: Int): Unit
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
    fun showHit(p: Point): Unit
    fun countStraight(): Int
    fun weLost(): Boolean
}
package Styx

import Styx.DrawnLines.Line
import java.awt.Point

interface iShow {
    fun showDrawMode(active: Boolean): Unit
    fun showLvl(value: Int): Unit
    fun showLives(value: Int): Unit
    fun showPercent(value: Int): Unit
    fun showScore(value: Int): Unit
}

interface iArena: iShow {
    val arenaMask: ArenaMask
    val drawnLines: DrawnLines
    suspend fun addWaypoint(p: Point, dir: Player.Direction): Unit
    suspend fun mkArea(): Unit
    fun isPosAvailable(p: Point): Boolean
    fun isOnEdge(p: Point): Boolean
    fun getPointType(p: Point): ArenaMask.MaskType
    fun showHit(p: Point): Unit
    fun weLost(): Boolean
}
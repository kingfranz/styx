package Styx

import java.awt.Color

class ColorCycle {
    private val scanner = BounceCount(20, 90)

    fun rndClr(): Color {
        val h = scanner() / 100.0f
        val s = 0.9f
        val b = 0.7f
        return Color.getHSBColor(h, s, b)
    }
}
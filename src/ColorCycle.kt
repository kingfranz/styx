package Styx

import java.awt.Color

class ColorCycle {
    companion object {
        private var scanner = 20
        private var goingDown = false

        fun rndClr(): Color {
            val h = scanner / 100.0f
            if (goingDown) {
                scanner -= 5
                if (scanner < 10) {
                    goingDown = false
                }
            } else {
                scanner += 7
                if (scanner > 90) {
                    goingDown = true
                }
            }
            val s = 0.9f
            val b = 0.7f
            return Color.getHSBColor(h, s, b)
        }
    }
}
package Styx

import java.awt.Graphics2D

class OneShotAnim(val numSteps: Int, val stepSz: Int) {
    var step = 0

    fun reset() {
        step = 0
    }

    operator fun invoke(g: Graphics2D, callback: (Graphics2D, Int) -> Unit): Boolean {
        callback(g, step)
        step += stepSz
        return step <= numSteps
    }
}
package Styx

class BounceCount(val low: Int,
                  val high: Int,
                  val stepUp: Int = 5,
                  val stepDown: Int = 7) {
    private var scanner = low
    private var goingDown = false

    operator fun invoke(): Int {
        if (goingDown) {
            scanner -= stepUp
            if (scanner < low) {
                goingDown = false
            }
        } else {
            scanner += stepDown
            if (scanner > high) {
                goingDown = true
            }
        }
        return scanner
    }
}
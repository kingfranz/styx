import Player.Direction
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.java.games.input.Controller
import net.java.games.input.Event
import net.java.games.input.EventQueue
import net.java.games.input.ControllerEnvironment

class Joystick() {
    private var controller: Controller? = null
    private var queue: EventQueue = EventQueue(100)
    var jsDir: Direction? = null
    var draw = false
    var special = false

    init {
        val controllers = ControllerEnvironment.getDefaultEnvironment().controllers
        for (ctrl in controllers) {
            if (ctrl.name == "8BitDo Ultimate 2C Wireless Controller") {
                controller = ctrl
                controller!!.poll()
                queue = controller!!.eventQueue
                break
            }
        }
    }

    fun toString(event: Event): String {
        val nano = event.nanos
        val compName = event.component.name
        val value = event.value
        var tmp: String = ""
        if (event.component.isAnalog)
            tmp = value.toString()
        else if (event.value == 1.0f)
            tmp = "pressed"
        else if (event.value == 0.0f)
            tmp = "released"
        return "${controller!!.name} at $nano: $compName $tmp"
    }

    suspend fun run(): Unit = coroutineScope {
        var xBlocked = false
        var yBlocked = false
        var event: Event = Event()
        launch {
            while (true) {
                controller!!.poll()
                //val toOrRet = queue.getNextEvent(event)
                // while (withTimeoutOrNull(5000L) { queue.getNextEvent(event) } != null) {
                while (queue!!.getNextEvent(event)) {
                    /*
                if (toOrRet == null || toOrRet == false) {
                    println("timeout ${jsDir} ${xBlocked} ${yBlocked}")
                    jsDir = null
                    xBlocked = false
                    yBlocked = false
                    delay(10)
                    continue
                }
*/
                    when (event.component.name) {
                        "rx" -> {
                            if (xBlocked)
                                continue
                            if (event.value == 0.0f) {
                                println("rx: 0 ${jsDir} ${xBlocked} ${yBlocked}")
                                yBlocked = false
                                jsDir = null
                            } else if (!xBlocked) {
                                yBlocked = true
                                if (event.value < 0.0) {
                                    jsDir = Direction.LEFT
                                } else {
                                    jsDir = Direction.RIGHT
                                }
                            }
                        }

                        "ry" -> {
                            if (yBlocked)
                                continue
                            if (event.value == 0.0f) {
                                println("ry: 0 ${jsDir} ${xBlocked} ${yBlocked}")
                                xBlocked = false
                                jsDir = null
                            } else if (!yBlocked) {
                                xBlocked = true
                                if (event.value < 0.0) {
                                    jsDir = Direction.UP
                                } else {
                                    jsDir = Direction.DOWN
                                }
                            }
                        }

                        "z" -> {
                            if (event.value > 0.5) {
                                draw = true
                            } else if (event.value < -0.5) {
                                draw = false
                            }
                        }

                        "rz" -> {
                            if (event.value > 0.5) {
                                special = true
                            } else if (event.value < -0.5) {
                                special = false
                            }
                        }
                    }
                }
                delay(10)
            }
        }
    }
}


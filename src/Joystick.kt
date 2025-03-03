import Player.Direction
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val mutex = Mutex(false)

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

    fun v2s(v: Float, off: String, low: String, high: String): String {
        return if (v < -0.5) low else if (v > 0.5) high else if (v == 0.0f) off else ""
    }

    suspend fun run(): Unit = coroutineScope {
        try {
            var xBlocked = false
            var yBlocked = false
            launch {
                while (true) {
                    try {
                        var event = Event()
                        controller!!.poll()
                        while (queue!!.getNextEvent(event)) {
                            var action = ""
                            //println("${event.component.name} ${event.value}")
                            when (event.component.name) {
                                "rx" -> {
                                    action = v2s(event.value * 10, "IDLE", "X-", "X+")
                                }

                                "ry" -> {
                                    action = v2s(event.value * 10, "IDLE", "Y-", "Y+")
                                }

                                "X" -> {
                                    action = v2s(event.value, "IDLE", "", "X-")
                                }

                                "Y" -> {
                                    action = v2s(event.value, "IDLE", "", "Y-")
                                }

                                "A" -> {
                                    action = v2s(event.value, "IDLE", "", "Y+")
                                }

                                "B" -> {
                                    action = v2s(event.value, "IDLE", "", "X+")
                                }

                                "x" -> {
                                    action = v2s(event.value * 10, "IDLE", "X-", "X+")
                                }

                                "y" -> {
                                    action = v2s(event.value * 10, "IDLE", "Y-", "Y+")
                                }

                                "pov" -> when (event.value) {
                                    in 0.1f..0.25f -> action = "Y+"
                                    in 0.25f..0.5f -> action = "X+"
                                    in 0.5f..0.75f -> action = "Y-"
                                    in 0.75f..1.0f -> action = "X-"
                                    else -> action = "IDLE"
                                }

                                "z" -> {
                                    action = v2s(event.value, "", "DRAW-", "DRAW+")
                                }

                                "rz" -> {
                                    action = v2s(event.value, "", "SPECIAL-", "SPECIAL+")
                                }
                            }
                            mutex.withLock {
                                when (action) {
                                    "IDLE" -> {
                                        xBlocked = false
                                        yBlocked = false
                                        jsDir = null
                                    }

                                    "X+" -> {
                                        if (!xBlocked) {
                                            yBlocked = true
                                            jsDir = Direction.RIGHT
                                        }
                                    }

                                    "X-" -> {
                                        if (!xBlocked) {
                                            yBlocked = true
                                            jsDir = Direction.LEFT
                                        }
                                    }

                                    "Y+" -> {
                                        if (!yBlocked) {
                                            xBlocked = true
                                            jsDir = Direction.DOWN
                                        }
                                    }

                                    "Y-" -> {
                                        if (!yBlocked) {
                                            xBlocked = true
                                            jsDir = Direction.UP
                                        }
                                    }

                                    "DRAW+" -> draw = true
                                    "DRAW-" -> draw = false
                                    "SPECIAL+" -> special = true
                                    "SPECIAL-" -> special = false
                                }
                            }
                        }
                        delay(10)
                    }
                    catch (e: Exception) {
                        println("Joystick2 exception: $e")
                    }
                }
            }
        }
        catch (e: Exception) {
            println("Joystick exception: $e")
        }
        finally {
            println("Joystick finally")
        }
    }
}


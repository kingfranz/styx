import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.java.games.input.Controller
import net.java.games.input.Event
import net.java.games.input.EventQueue

data class JoystickEvent(val name: String, val value: Float)

class Joystick() {
    var controller: Controller? = null
    var queue: EventQueue? = null
    var event = Event()
    val joystickChannel = Channel<JoystickEvent>(10)

    init {
        val controllers = net.java.games.input.ControllerEnvironment.getDefaultEnvironment().controllers
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
        launch {
            while (true) {
                controller!!.poll()
                while (queue!!.getNextEvent(event)) {
                    joystickChannel.send(JoystickEvent(event.component.name, event.value))
                }
                delay(10)
            }
        }
    }
}


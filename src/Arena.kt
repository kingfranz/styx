import ArenaMask.MaskType
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.BasicStroke
import java.awt.Canvas
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Polygon
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.image.BufferedImage
import java.util.LinkedList

///////////////////////////////////////////////////////////////////////////////

class Arena(val parent: iShow) : Canvas(), iArena, iTargets {
    val edgeSz = 20
    val sprite = Sprite(this, edgeSz)
    val player = Player(this)
    val keys = mutableMapOf<Int, Boolean>()
    val lines = LinkedList<Pair<Point, Player.Direction>>() // (x, y)
    var xArr: IntArray = IntArray(1000) { i -> i }
    var yArr: IntArray = IntArray(1000) { i -> i }
    var arrSize = 0
    val areas = LinkedList<Polygon>()
    val arenaSz = 1000
    override val arenaMask = ArenaMask(arenaSz, arenaSz)

    init {
        isFocusable = true
        preferredSize = java.awt.Dimension(arenaSz + 2 * edgeSz, arenaSz + 2 * edgeSz)
        minimumSize = java.awt.Dimension(arenaSz + 2 * edgeSz, arenaSz + 2 * edgeSz)
        maximumSize = java.awt.Dimension(arenaSz + 2 * edgeSz, arenaSz + 2 * edgeSz)
        size = java.awt.Dimension(arenaSz + 2 * edgeSz, arenaSz + 2 * edgeSz)
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                when (e.keyChar) {
                    'q' -> System.exit(0)
                }
            }

            override fun keyPressed(e: KeyEvent) {
                //println("keyPressed-a: ${e.keyCode}")
                keys[e.keyCode] = true
                runBlocking {
                    when (e.keyCode) {
                        KeyEvent.VK_LEFT -> player.kbdMove(
                            Player.Direction.LEFT,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true
                        )

                        KeyEvent.VK_RIGHT -> player.kbdMove(
                            Player.Direction.RIGHT,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true
                        )

                        KeyEvent.VK_UP -> player.kbdMove(
                            Player.Direction.UP,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true
                        )

                        KeyEvent.VK_DOWN -> player.kbdMove(
                            Player.Direction.DOWN,
                            keys[KeyEvent.VK_SHIFT] == true,
                            keys[KeyEvent.VK_CONTROL] == true
                        )
                    }
                }
            }

            override fun keyReleased(e: KeyEvent) {
                //println("keyReleased-a: ${e.keyCode}")
                keys[e.keyCode] = false
            }
        })
        runBlocking { reset() }
        requestFocus()
    }

    suspend fun reset() {
        sprite.reset()
        player.reset()
        clearLines()
        clearAreas()
        arenaMask.fillRect(0, 0, arenaSz, arenaSz, MaskType.EMPTY_CLR)
        arenaMask.stroke = BasicStroke(1f)
        arenaMask.drawRect(0, 0, arenaSz, arenaSz, MaskType.WALL_CLR)
    }

    override fun showSpeed(value: Int) {
        parent.showSpeed(value)
    }

    override fun showDrawMode(active: Boolean) {
        parent.showDrawMode(active)
    }

    override fun showLoc(value: String) {
        parent.showLoc(value)
    }

    override fun getLines(): Triple<Int, IntArray, IntArray> {
        return Triple(arrSize, xArr, yArr)
    }

    override fun getDrawnAreas(): LinkedList<Polygon> {
        return areas
    }

    override fun addLeg(p: Point, dir: Player.Direction) {
        if (arrSize > 0) {
            if (xArr[arrSize - 1] == p.x && yArr[arrSize - 1] == p.y) {
                return
            }
            if (arrSize > 1) {
                val x1 = xArr[arrSize - 2]
                val y1 = yArr[arrSize - 2]
                val x2 = xArr[arrSize - 1]
                val y2 = yArr[arrSize - 1]
                val x3 = p.x
                val y3 = p.y

                if ((y2 - y1) * (x3 - x2) == (y3 - y2) * (x2 - x1)) {
                    xArr[arrSize - 1] = p.x
                    yArr[arrSize - 1] = p.y
                    lines.removeLast()
                    lines.add(Pair(p, dir))
                    return
                }
            }
        }
        if (arrSize == xArr.size) {
            xArr += IntArray(1000) { i -> i }
            yArr += IntArray(1000) { i -> i }
        }
        xArr[arrSize] = p.x
        yArr[arrSize] = p.y
        arrSize++
        lines.add(Pair(p, dir))
        if (arrSize > 1) {
            //arenaMask.color = MaskType.LINE_CLR.value
            //arenaMask.stroke = BasicStroke(1f)
            arenaMask.drawLine(
                Point(xArr[arrSize - 2], yArr[arrSize - 2]),
                Point(p.x, p.y),
                MaskType.LINE_CLR)
        }
    }

    override fun getLeg(i: Int): Pair<Point, Player.Direction> {
        return lines[i]
    }

    override fun numLegs(): Int {
        return lines.size
    }

    override fun clearLines() {
        lines.clear()
        arrSize = 0
    }

    override fun clearAreas() {
        areas.clear()
    }

    fun calcArea(): Int {
        var emptyCnt = 0
        var areaCnt = 0
        var wallCnt = 0
        var misc = 0
        var startTS = java.time.Instant.now()
        for (i in 0 until arenaSz) {
            for (j in 0 until arenaSz) {
                val aa = arenaMask.get(i, j)
                when (aa) {
                    MaskType.EMPTY_CLR -> emptyCnt++
                    MaskType.WALL_CLR -> wallCnt++
                    MaskType.AREA_CLR -> areaCnt++
                    else -> misc++
                }
            }
        }
        var endTS = java.time.Instant.now()
        println("empty: $emptyCnt, wall: $wallCnt, area: $areaCnt ${endTS.toEpochMilli() - startTS.toEpochMilli()} ms")
        return (areaCnt.toDouble() / (emptyCnt + wallCnt + areaCnt).toDouble() * 100.0).toInt()
    }

    fun calcPolyArea(poly: Polygon): Int {
        var areaCnt = 0
        for (y in 0 until arenaSz) {
            for (x in 0 until arenaSz) {
                if(poly.contains(x, y)) {
                    areaCnt++
                }
            }
        }
        return (areaCnt.toDouble() / (arenaSz*arenaSz).toDouble() * 100.0).toInt()
    }

    fun sample(p: Point): Set<Player.Direction> {
        val dirs = mutableSetOf<Player.Direction>()
        if (p.x > 0 && (getPointType(Point(p.x - 1, p.y)) == MaskType.WALL_CLR)
        ) {
            dirs.add(Player.Direction.LEFT)
        }
        if (p.x < arenaSz - 1 && (getPointType(Point(p.x + 1, p.y)) == MaskType.WALL_CLR)
        ) {
            dirs.add(Player.Direction.RIGHT)
        }
        if (p.y > 0 && (getPointType(Point(p.x, p.y - 1)) == MaskType.WALL_CLR)
        ) {
            dirs.add(Player.Direction.UP)
        }
        if (p.y < arenaSz - 1 && (getPointType(Point(p.x, p.y + 1)) == MaskType.WALL_CLR)
        ) {
            dirs.add(Player.Direction.DOWN)
        }
        return dirs
    }

    var lastDir = Player.Direction.RIGHT

    fun followWall(start: Point, clockvice: Boolean, first: Boolean = false): Vec {
        var walls = sample(start)
        if(clockvice) {
            if(walls.contains(Player.Direction.UP) && (first || lastDir != Player.Direction.DOWN)) {
                lastDir = Player.Direction.UP
                return Vec(0, -1)
            } else if(walls.contains(Player.Direction.RIGHT) && (first || lastDir != Player.Direction.LEFT)) {
                lastDir = Player.Direction.RIGHT
                return Vec(1, 0)
            }
            else if(first) {
                // error
                println("followWall.cv: no start")
                return Vec(0, 0)
            }
            else if(walls.contains(Player.Direction.DOWN) && lastDir != Player.Direction.UP) {
                lastDir = Player.Direction.DOWN
                return Vec(0, 1)
            }
            else if(walls.contains(Player.Direction.LEFT) && lastDir != Player.Direction.RIGHT) {
                lastDir = Player.Direction.LEFT
                return Vec(-1, 0)
            }
            else {
                // error
                println("followWall.cv: no end")
                return Vec(0, 0)
            }
        }
        else {
            if(walls.contains(Player.Direction.DOWN) && (first || lastDir != Player.Direction.UP)) {
                lastDir = Player.Direction.DOWN
                return Vec(0, 1)
            } else if(walls.contains(Player.Direction.LEFT) && (first || lastDir != Player.Direction.RIGHT)) {
                lastDir = Player.Direction.LEFT
                return Vec(-1, 0)
            }
            else if(first) {
                // error
                println("followWall.ccv: no start")
                return Vec(0, 0)
            }
            else if(walls.contains(Player.Direction.RIGHT) && lastDir != Player.Direction.LEFT) {
                lastDir = Player.Direction.RIGHT
                return Vec(1, 0)
            }
            else if(walls.contains(Player.Direction.UP) && lastDir != Player.Direction.DOWN) {
                lastDir = Player.Direction.UP
                return Vec(0, -1)
            }
            else {
                // error
                println("followWall.ccv: no end")
                return Vec(0, 0)
            }
        }
    }

    fun finalizePath(): Polygon? {
        if(lines.size < 2) {
            return null
        }
        val start = lines.first().first
        val stop = lines.last().first
        arenaMask.set(start, MaskType.WALL_CLR)
        arenaMask.set(stop, MaskType.WALL_CLR)

        //-------------------------------------------
        // cvPath
        //-------------------------------------------
        var delta = followWall(start, true, true)
        var traveller = start
        val cvPath = mutableListOf<Point>()
        cvPath.add(traveller)
        while(delta != Vec(0, 0) && traveller != stop) {
            traveller = traveller.translate(delta)
            cvPath.add(traveller)
            if (cvPath.size > 100) {
                throw Exception("finalizePath.cv: path is too long")
            }
            var tmp = Point(0, 0)
            do {
                tmp = delta
                delta = followWall(traveller, true)
                if(tmp == delta)
                    traveller = traveller.translate(delta)
            } while(tmp == delta && traveller != stop)
        }
        if(delta == Vec(0, 0)) {
            // error
            println("finalizePath.cv: no end")
            return null
        }
        var cvPoly: Polygon? = null
        if(traveller == stop) {
            lines.reversed().forEach { p ->
                cvPath.add(p.first)
            }
            val xs = IntArray(cvPath.size) { i -> cvPath[i].x }
            val ys = IntArray(cvPath.size) { i -> cvPath[i].y }
            cvPoly = Polygon(xs, ys, cvPath.size)
        }


        //-------------------------------------------
        // ccvPath
        //-------------------------------------------
        delta = followWall(start, false, true)
        traveller = start
        val ccvPath = mutableListOf<Point>()
        cvPath.add(traveller)
        while(delta != Vec(0, 0) && traveller != stop) {
            traveller = traveller.translate(delta)
            ccvPath.add(traveller)
            if (ccvPath.size > 100) {
                throw Exception("finalizePath.ccv: path is too long")
            }
            var tmp = Point(0, 0)
            do {
                tmp = delta
                delta = followWall(traveller, false)
                if(tmp == delta)
                    traveller = traveller.translate(delta)
            } while(tmp == delta && traveller != stop)
        }
        if(delta == Vec(0, 0)) {
            // error
            println("finalizePath.ccv: no end")
            return null
        }
        var ccvPoly: Polygon? = null
        if(traveller == stop) {
            lines.reversed().forEach { p ->
                ccvPath.add(p.first)
            }
            val xs = IntArray(ccvPath.size) { i -> ccvPath[i].x }
            val ys = IntArray(ccvPath.size) { i -> ccvPath[i].y }
            ccvPoly = Polygon(xs, ys, ccvPath.size)
        }
        val cvArea = cvPoly?.let { calcPolyArea(it) } ?: 0
        val ccvArea = ccvPoly?.let { calcPolyArea(it) } ?: 0
        println("cvArea: $cvArea, ccvArea: $ccvArea")
        return if(cvArea < ccvArea) cvPoly else ccvPoly
    }

    override fun mkArea() {
        //val xs = IntArray(lines.size) { i -> lines[i].first.x }
        //val ys = IntArray(lines.size) { i -> lines[i].first.y }
        //val poly = Polygon(xs, ys, lines.size)
        val poly = finalizePath()
        if(poly != null) {
            println(poly.str())
            areas.add(poly)
            arenaMask.fPoly(poly)
            // draw outline
            arenaMask.drawPoly(poly, MaskType.WALL_CLR)
        }
        // cleanup
        lines.clear()
        arrSize = 0
        showPercent(calcArea())
    }

    override fun getPointType(p: Point): MaskType {
        try {
            if (p.x < 0 || p.y < 0 || p.x >= arenaSz || p.y >= arenaSz) {
                return MaskType.ERROR_CLR
            }
            val aa = arenaMask.get(p.x, p.y)
            return aa
        } catch (e: Exception) {
            println("getPointType: $p $e")
            return MaskType.ERROR_CLR
        }
    }

    override fun isPosAvailable(p: Point): Boolean {
        val aa = getPointType(p)
        if (aa == MaskType.ERROR_CLR)
            return false
        return aa == MaskType.EMPTY_CLR || aa == MaskType.WALL_CLR
    }

    override fun isOnEdge(p: Point): Boolean {
        val aa = getPointType(p)
        if (aa == MaskType.ERROR_CLR)
            return false
        return aa == MaskType.WALL_CLR
    }

    fun drawAxis(g: Graphics2D) {
        g.color = Color.RED
        g.stroke = java.awt.BasicStroke(4.0f)
        g.drawLine(0, 0, 100, 0)
        g.color = Color.BLUE
        g.drawLine(0, 0, 0, 100)
    }

    fun drawEdge(g: Graphics2D) {
        g.color = Color.BLACK
        g.stroke = java.awt.BasicStroke(2.0f)
        g.drawLine(sprite.minX, sprite.minY, sprite.maxX, sprite.minY)
        g.drawLine(sprite.maxX, sprite.minY, sprite.maxX, sprite.maxY)
        g.drawLine(sprite.maxX, sprite.maxY, sprite.minX, sprite.maxY)
        g.drawLine(sprite.minX, sprite.maxY, sprite.minX, sprite.minY)
    }

    override fun update(g: Graphics) {
        //runBlocking { sprite.step() }
    }

    override fun showPercent(value: Int) {
        parent.showPercent(value)
    }

    var hitStage = 0
    var hitPoint = Point(-1, -1)
    override fun showHit(p: Point) {
        hitStage = 1
        hitPoint = p
    }

    suspend fun displayHit(g: Graphics2D) {
        if (hitStage > 0) {
            g.color = Color.RED
            g.stroke = java.awt.BasicStroke(10.0f)
            drawCircle(g, hitPoint.x, hitPoint.y, hitStage*10)
            hitStage++
            if (hitStage > 50) {
                hitStage = 0
                delay(2000L)
                reset()
            }
        }
    }

    override fun paint(g: Graphics) {
        try {
            val g2d = g as Graphics2D
            g2d.color = Color.WHITE
            g2d.fillRect(0, 0, width, height)

            // draw the sprite
            val spriteGraph = g2d.create() as Graphics2D
            spriteGraph.translate(edgeSz, edgeSz)
            drawEdge(spriteGraph)
            runBlocking { sprite.draw(spriteGraph) }
            runBlocking { displayHit(spriteGraph) }
            spriteGraph.dispose()

            // draw the player
            val playerGraph = g2d.create() as Graphics2D
            playerGraph.translate(edgeSz, edgeSz)
            runBlocking { player.draw(playerGraph) }

            // draw the draw-track
            val (num, xs, ys) = getLines()
            if(num > 0) {
                playerGraph.color = Color.RED
                playerGraph.stroke = java.awt.BasicStroke(2.0f)
                playerGraph.drawPolyline(xs, ys, num)
                val last = Point(xs[num-1], ys[num-1])
                if(last != player.pos.xy) {
                    playerGraph.drawLine(last.x, last.y, player.pos.xy.x, player.pos.xy.y)
                }
            }

            // show areas
            for (a in areas) {
                playerGraph.color = Color.BLACK
                playerGraph.stroke = BasicStroke(1f)
                playerGraph.drawPolygon(a)
                playerGraph.color = Color.GRAY
                playerGraph.fillPolygon(a)
            }
            playerGraph.dispose()
        }
        catch (e: Exception) {
            println("Arena.paint: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun run(): Unit = coroutineScope {
        try {
            launch { sprite.run() }
            launch {
                try {
                    player.joystick.run()
                }
                catch (e: Exception) {
                    println("Arena.run: joystick: ${e.message}")
                    e.printStackTrace()
                }
            }
            launch {
                try {
                    player.makeMoves()
                }
                catch (e: Exception) {
                    println("Arena.run: makeMoves: ${e.message}")
                    e.printStackTrace()
                }
            }
            createBufferStrategy(2)
            val strategy = bufferStrategy
            launch(Dispatchers.Default + CoroutineName("Arena")) {
                while (true) {
                    try {
                        delay(sprite.spriteSpeed)
                        do {
                            do {
                                val g = strategy.drawGraphics
                                try {
                                    update(g)
                                    paint(g)
                                } finally {
                                    g.dispose()
                                }
                            } while (strategy.contentsRestored())
                            strategy.show()
                        } while (strategy.contentsLost())
                    }
                    catch (e: Exception) {
                        println("Arena.run2: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
        catch (e: Exception) {
            println("Arena.run: ${e.message}")
            e.printStackTrace()
        }
        finally {
            println("Arena.run: finally")
        }
    }
}

package Styx

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Button
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

///////////////////////////////////////////////////////////////////////////////

class ClientArea(): JPanel(), iShow {
    val resetButton = Button("Reset")
    val lvlLbl = JLabel("Lvl:1")
    val scoreLbl = JLabel("Score:0")
    val drawLbl = JLabel("")
    val lifesLbl = JLabel("")
    val percentLbl = JLabel("0%")
    val arena = Arena(this)
    val topPanel = JPanel(FlowLayout())
    val uiFont = Font("Arial", Font.BOLD, 30)

    init {
        background = Color.GRAY
        layout = BorderLayout()
        resetButton.font = uiFont
        lvlLbl.font = uiFont
        lvlLbl.border = EmptyBorder(0, 20, 0, 20)
        scoreLbl.font = uiFont
        scoreLbl.border = EmptyBorder(0, 20, 0, 20)
        drawLbl.font = uiFont
        drawLbl.border = EmptyBorder(0, 0, 0, 20)
        lifesLbl.font = uiFont
        lifesLbl.border = EmptyBorder(0, 0, 0, 20)
        percentLbl.font = uiFont
        add(topPanel, BorderLayout.NORTH)
        topPanel.add(resetButton)
        topPanel.add(lvlLbl)
        topPanel.add(scoreLbl)
        topPanel.add(drawLbl)
        topPanel.add(lifesLbl)
        topPanel.add(percentLbl)
        add(arena, BorderLayout.CENTER)
        isVisible = true
        resetButton.addActionListener {
           runBlocking { arena.reset() }
        }
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                when (e.keyChar) {
                    'q' -> System.exit(0)
                }
            }

            override fun keyPressed(e: KeyEvent) {
                //println("keyPressed-c: ${e.keyCode}")
                arena.dispatchEvent(e)
                if (arena.player.pos.drawing)
                    drawLbl.text = "Draw"
                else
                    drawLbl.text = ""
                drawLbl.repaint()
            }

            override fun keyReleased(e: KeyEvent) {
                //println("keyReleased-c: ${e.keyCode}")
                arena.dispatchEvent(e)
            }
        })
    }

    override fun showLvl(value: Int) {
        lvlLbl.text = "Level:${value.toString()}"
    }

    override fun showScore(value: Int) {
        scoreLbl.text = "Score:${value.toString()}"
    }

    override fun showDrawMode(active: Boolean) {
        drawLbl.text = if (active) "DRAW" else ""
    }

    override fun showLives(value: Int) {
        lifesLbl.text = "Lifes:${value}"
    }

    override fun showPercent(value: Int) {
        percentLbl.text = "$value%"
    }

    override fun paint(g: Graphics?) {
        try {
            super.paint(g)
            arena.repaint()
        }
        catch (e: Exception) {
            println("ClientArea.paint: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun run(): Unit = coroutineScope {
        try {
            arena.run()
        }
        catch (e: Exception) {
            println("ClientArea.run: ${e.message}")
            e.printStackTrace()
        }
        finally {
            println("ClientArea.run: finally")
        }
    }
}

package com.jeremy.cozyfarm

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private data class Crop(
        val name: String,
        val seedCost: Int,
        val sellPrice: Int,
        val growthDays: Int,
        val readyEmoji: String
    )

    private val crops = mapOf(
        "Wheat" to Crop("Wheat", 10, 25, 2, "🌾"),
        "Carrot" to Crop("Carrot", 15, 40, 3, "🥕"),
        "Pumpkin" to Crop("Pumpkin", 25, 80, 4, "🎃")
    )

    private data class Tile(
        var state: Int = 0,
        var crop: String = "",
        var progress: Int = 0,
        var watered: Boolean = false
    )

    private val cols = 6
    private val rows = 5
    private val tiles = MutableList(rows * cols) { Tile() }

    private var coins = 50
    private var day = 1
    private var tool = "Till"
    private var selectedSeed = "Wheat"

    // Character position, in tile units. 2.5, 2.5 is the middle tile.
    private var charX = 2.5f
    private var charY = 2.5f
    private var moveDx = 0
    private var moveDy = 0

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var toolText: TextView
    private lateinit var farmView: FarmView
    private val toolButtons = mutableMapOf<String, Button>()
    private val seedButtons = mutableMapOf<String, Button>()
    private val handler = Handler(Looper.getMainLooper())

    private val moveRunnable = object : Runnable {
        override fun run() {
            if (moveDx != 0 || moveDy != 0) {
                val speed = 0.055f
                charX = min(max(charX + moveDx * speed, 0.5f), cols - 0.5f)
                charY = min(max(charY + moveDy * speed, 0.5f), rows - 0.5f)
                farmView.invalidate()
            }
            handler.postDelayed(this, 16)
        }
    }

    private inner class FarmView(context: Context) : View(context) {
        private val tilePaint = Paint()
        private val borderPaint = Paint().apply {
            color = Color.parseColor("#558B2F")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        private val highlightPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        private val emojiPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        private val charPaint = Paint().apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val tileW = width.toFloat() / cols
            val tileH = height.toFloat() / rows
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val tile = tiles[r * cols + c]
                    tilePaint.color = when (tile.state) {
                        1 -> Color.parseColor("#8D6E63")
                        2 -> if (tile.watered) Color.parseColor("#6D4C41") else Color.parseColor("#8D6E63")
                        else -> Color.parseColor("#7CB342")
                    }
                    tilePaint.style = Paint.Style.FILL
                    canvas.drawRect(c * tileW, r * tileH, (c + 1) * tileW, (r + 1) * tileH, tilePaint)
                    canvas.drawRect(c * tileW, r * tileH, (c + 1) * tileW, (r + 1) * tileH, borderPaint)
                    val emoji = when (tile.state) {
                        0 -> "🌱"
                        1 -> ""
                        else -> tileEmoji(tile)
                    }
                    if (emoji.isNotEmpty()) {
                        emojiPaint.textSize = min(tileW, tileH) * 0.5f
                        canvas.drawText(
                            emoji,
                            c * tileW + tileW / 2,
                            r * tileH + tileH / 2 + emojiPaint.textSize / 3,
                            emojiPaint
                        )
                    }
                    if (tile.watered && tile.state == 2) {
                        emojiPaint.textSize = min(tileW, tileH) * 0.28f
                        canvas.drawText(
                            "💧",
                            c * tileW + tileW - emojiPaint.textSize / 2,
                            r * tileH + emojiPaint.textSize,
                            emojiPaint
                        )
                    }
                }
            }
            val charCol = charX.toInt()
            val charRow = charY.toInt()
            canvas.drawRect(
                charCol * tileW, charRow * tileH,
                (charCol + 1) * tileW, (charRow + 1) * tileH,
                highlightPaint
            )
            charPaint.textSize = min(tileW, tileH) * 0.85f
            canvas.drawText(
                "🧑‍🌾",
                charX * tileW,
                charY * tileH + charPaint.textSize / 3,
                charPaint
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("cozy_farm", MODE_PRIVATE)
        loadGame()

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "🌻 Our Cozy Farm"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(title)

        statusText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(statusText)

        toolText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(toolText)

        val toolRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Till", "Plant", "Water", "Harvest", "Clear").forEach { name ->
            val b = Button(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    tool = name
                    refreshAll()
                }
            }
            toolButtons[name] = b
            toolRow.addView(b)
        }
        root.addView(toolRow)

        val seedLabel = TextView(this).apply {
            text = "Seeds (tap one, then use Plant):"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(seedLabel)

        val seedRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        crops.values.forEach { crop ->
            val b = Button(this).apply {
                text = "${crop.name} ${crop.seedCost}c"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    selectedSeed = crop.name
                    tool = "Plant"
                    refreshAll()
                }
            }
            seedButtons[crop.name] = b
            seedRow.addView(b)
        }
        root.addView(seedRow)

        farmView = FarmView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(300)
            )
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(farmView)

        val moveLabel = TextView(this).apply {
            text = "Walk with the arrows, stand on a tile, then tap Use Tool."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
        }
        root.addView(moveLabel)

        val upRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        upRow.addView(dpadButton("⬆️", 0, -1))
        root.addView(upRow)

        val midRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        midRow.addView(dpadButton("⬅️", -1, 0))
        val useButton = Button(this).apply {
            text = "Use Tool"
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { useToolOnCurrentTile() }
        }
        midRow.addView(useButton)
        midRow.addView(dpadButton("➡️", 1, 0))
        root.addView(midRow)

        val downRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        downRow.addView(dpadButton("⬇️", 0, 1))
        root.addView(downRow)

        val nextDay = Button(this).apply {
            text = "☀️ Next Day"
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { nextDay() }
        }
        root.addView(nextDay)

        val reset = Button(this).apply {
            text = "Start over"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Start over?")
                    .setMessage("This clears your farm, coins, and day.")
                    .setPositiveButton("Yes, start over") { _, _ ->
                        prefs.edit().clear().apply()
                        coins = 50
                        day = 1
                        charX = 2.5f
                        charY = 2.5f
                        for (i in tiles.indices) tiles[i] = Tile()
                        saveGame()
                        refreshAll()
                        Toast.makeText(this@MainActivity, "Fresh farm!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Keep my farm", null)
                    .show()
            }
        }
        root.addView(reset)

        val help = TextView(this).apply {
            text = "How to play: Hold an arrow to walk. Stand on grass and Use Tool with Till, then Plant a seed, Water it, tap Next Day to grow it. Water again each day or it will not grow. Harvest when you see 🌾, 🥕, or 🎃. Wheat takes 2 days, Carrot 3, Pumpkin 4. Your farm and your character position save automatically."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(help)

        setContentView(scroll)
        refreshAll()
        handler.post(moveRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(moveRunnable)
        saveGame()
        super.onDestroy()
    }

    private fun dpadButton(label: String, dx: Int, dy: Int): Button {
        return Button(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(56))
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        moveDx = dx
                        moveDy = dy
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (moveDx == dx && moveDy == dy) {
                            moveDx = 0
                            moveDy = 0
                        }
                        true
                    }
                    else -> true
                }
            }
        }
    }

    private fun dp(v: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private fun currentTileIndex(): Int {
        val c = min(max(charX.toInt(), 0), cols - 1)
        val r = min(max(charY.toInt(), 0), rows - 1)
        return r * cols + c
    }

    private fun useToolOnCurrentTile() {
        onTileClicked(currentTileIndex())
    }

    private fun onTileClicked(index: Int) {
        val tile = tiles[index]
        when (tool) {
            "Till" -> {
                if (tile.state == 0) {
                    tile.state = 1
                    Toast.makeText(this, "Tilled! Now plant a seed.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Already tilled or planted.", Toast.LENGTH_SHORT).show()
                }
            }
            "Plant" -> {
                val crop = crops[selectedSeed]!!
                if (tile.state != 1) {
                    Toast.makeText(this, "Till the soil first.", Toast.LENGTH_SHORT).show()
                } else if (coins < crop.seedCost) {
                    Toast.makeText(this, "Not enough coins for $selectedSeed.", Toast.LENGTH_SHORT).show()
                } else {
                    coins -= crop.seedCost
                    tile.state = 2
                    tile.crop = selectedSeed
                    tile.progress = 0
                    tile.watered = false
                    Toast.makeText(this, "Planted $selectedSeed! Water it.", Toast.LENGTH_SHORT).show()
                }
            }
            "Water" -> {
                if (tile.state != 2) {
                    Toast.makeText(this, "Plant something first.", Toast.LENGTH_SHORT).show()
                } else if (tile.watered) {
                    Toast.makeText(this, "Already watered today.", Toast.LENGTH_SHORT).show()
                } else {
                    tile.watered = true
                    Toast.makeText(this, "Watered!", Toast.LENGTH_SHORT).show()
                }
            }
            "Harvest" -> {
                val crop = crops[tile.crop]
                if (tile.state == 2 && crop != null && tile.progress >= crop.growthDays) {
                    coins += crop.sellPrice
                    tile.state = 1
                    tile.crop = ""
                    tile.progress = 0
                    tile.watered = false
                    Toast.makeText(this, "Harvested! +${crop.sellPrice} coins.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Not ready yet. Water it and wait.", Toast.LENGTH_SHORT).show()
                }
            }
            "Clear" -> {
                tile.state = 0
                tile.crop = ""
                tile.progress = 0
                tile.watered = false
            }
        }
        saveGame()
        refreshAll()
    }

    private fun nextDay() {
        var grew = 0
        for (tile in tiles) {
            if (tile.state == 2 && tile.watered) {
                tile.progress += 1
                grew += 1
            }
            tile.watered = false
        }
        day += 1
        saveGame()
        refreshAll()
        if (grew > 0) {
            Toast.makeText(this, "Day $day! $grew crops grew.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Day $day! No crops grew. Remember to water them.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tileEmoji(tile: Tile): String {
        if (tile.state == 0) return "🌱"
        if (tile.state == 1) return ""
        val crop = crops[tile.crop] ?: return "🌱"
        return when {
            tile.progress >= crop.growthDays -> crop.readyEmoji
            tile.progress == 0 -> "🌱"
            else -> "🌿"
        }
    }

    private fun refreshAll() {
        statusText.text = "Day $day  •  🪙 $coins coins"
        toolText.text = "Tool: $tool" + if (tool == "Plant") " ($selectedSeed)" else ""
        toolButtons.forEach { (name, b) ->
            b.setBackgroundColor(if (name == tool) Color.parseColor("#A5D6A7") else Color.parseColor("#DDDDDD"))
        }
        seedButtons.forEach { (name, b) ->
            b.setBackgroundColor(if (name == selectedSeed) Color.parseColor("#FFE082") else Color.parseColor("#DDDDDD"))
        }
        if (::farmView.isInitialized) farmView.invalidate()
    }

    private fun saveGame() {
        val tileString = tiles.joinToString(";") {
            "${it.state},${it.crop},${it.progress},${if (it.watered) 1 else 0}"
        }
        prefs.edit()
            .putInt("coins", coins)
            .putInt("day", day)
            .putString("tiles", tileString)
            .putFloat("charX", charX)
            .putFloat("charY", charY)
            .apply()
    }

    private fun loadGame() {
        coins = prefs.getInt("coins", 50)
        day = prefs.getInt("day", 1)
        charX = prefs.getFloat("charX", 2.5f)
        charY = prefs.getFloat("charY", 2.5f)
        val saved = prefs.getString("tiles", null) ?: return
        val parts = saved.split(";")
        for (i in parts.indices) {
            if (i >= tiles.size) break
            val f = parts[i].split(",")
            if (f.size < 4) continue
            tiles[i] = Tile(
                state = f[0].toIntOrNull() ?: 0,
                crop = f[1],
                progress = f[2].toIntOrNull() ?: 0,
                watered = f[3] == "1"
            )
        }
    }
}

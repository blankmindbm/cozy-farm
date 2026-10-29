package com.jeremy.cozyfarm

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

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

    private data class Animal(
        var type: String,
        var col: Int,
        var row: Int,
        var ready: Boolean = false
    )

    // World map: 10 wide, 8 tall.
    private val cols = 10
    private val rows = 8
    private val tiles = MutableList(rows * cols) { Tile() }
    private val animals = mutableListOf<Animal>()

    private val penTiles = listOf(
        Pair(8, 5), Pair(9, 5), Pair(8, 6),
        Pair(9, 6), Pair(8, 7), Pair(9, 7)
    )

    private var coins = 50
    private var day = 1
    private var tool = "Till"
    private var selectedSeed = "Wheat"

    private var charX = 2.5f
    private var charY = 1.5f
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
                val nextX = min(max(charX + moveDx * speed, 0.5f), cols - 0.5f)
                if (!isBlocked(nextX.toInt(), charY.toInt())) charX = nextX
                val nextY = min(max(charY + moveDy * speed, 0.5f), rows - 0.5f)
                if (!isBlocked(charX.toInt(), nextY.toInt())) charY = nextY
                farmView.invalidate()
            }
            handler.postDelayed(this, 16)
        }
    }

    private fun isHouseTile(c: Int, r: Int) = c in 0..1 && r in 0..1
    private fun isPondTile(c: Int, r: Int) = c in 8..9 && r in 0..1
    private fun isMineTile(c: Int, r: Int) = c == 9 && r == 3
    private fun isPenTile(c: Int, r: Int) = penTiles.contains(Pair(c, r))
    private fun isFieldTile(c: Int, r: Int) = c in 2..7 && r in 2..5
    private fun isBlocked(c: Int, r: Int) = isHouseTile(c, r) || isPondTile(c, r) || isMineTile(c, r)

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

        private fun tileCenter(c: Int, r: Int, tileW: Float, tileH: Float, topY: Float): Pair<Float, Float> {
            val cx = width / 2f + (c - r) * tileW / 2
            val cy = topY + (c + r) * tileH / 2 + tileH / 2
            return Pair(cx, cy)
        }

        private fun drawDiamond(canvas: Canvas, cx: Float, cy: Float, tileW: Float, tileH: Float, paint: Paint) {
            val path = Path()
            path.moveTo(cx, cy - tileH / 2)
            path.lineTo(cx + tileW / 2, cy)
            path.lineTo(cx, cy + tileH / 2)
            path.lineTo(cx - tileW / 2, cy)
            path.close()
            canvas.drawPath(path, paint)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val tileW = width.toFloat() * 2f / (cols + rows)
            val tileH = tileW / 2
            val mapHeight = (cols + rows + 1) * tileH / 2
            val topY = (height - mapHeight) / 2
            for (sum in 0 until cols + rows - 1) {
                for (c in 0 until cols) {
                    val r = sum - c
                    if (r < 0 || r >= rows) continue
                    val tile = tiles[r * cols + c]
                    val bg = when {
                        isHouseTile(c, r) -> Color.parseColor("#FFE0B2")
                        isPondTile(c, r) -> Color.parseColor("#4FC3F7")
                        isMineTile(c, r) -> Color.parseColor("#9E9E9E")
                        isPenTile(c, r) -> Color.parseColor("#C5E1A5")
                        isFieldTile(c, r) -> when (tile.state) {
                            1, 2 -> Color.parseColor("#8D6E63")
                            else -> Color.parseColor("#AED581")
                        }
                        else -> Color.parseColor("#7CB342")
                    }
                    val (cx, cy) = tileCenter(c, r, tileW, tileH, topY)
                    tilePaint.color = bg
                    tilePaint.style = Paint.Style.FILL
                    drawDiamond(canvas, cx, cy, tileW, tileH, tilePaint)
                    drawDiamond(canvas, cx, cy, tileW, tileH, borderPaint)
                    val emoji = when {
                        isHouseTile(c, r) -> "🏠"
                        isPondTile(c, r) -> "💧"
                        isMineTile(c, r) -> "⛏️"
                        isFieldTile(c, r) -> when (tile.state) {
                            0 -> ""
                            1 -> ""
                            else -> tileEmoji(tile)
                        }
                        else -> ""
                    }
                    if (emoji.isNotEmpty()) {
                        emojiPaint.textSize = tileH * 0.9f
                        canvas.drawText(emoji, cx, cy + emojiPaint.textSize / 3, emojiPaint)
                    }
                    if (isFieldTile(c, r) && tile.watered && tile.state == 2) {
                        emojiPaint.textSize = tileH * 0.5f
                        canvas.drawText("💧", cx + tileW / 4, cy - tileH / 4, emojiPaint)
                    }
                }
            }
            for (animal in animals) {
                val (cx, cy) = tileCenter(animal.col, animal.row, tileW, tileH, topY)
                val emoji = if (animal.type == "Cow") "🐄" else "🐔"
                emojiPaint.textSize = tileH * 0.9f
                canvas.drawText(emoji, cx, cy + emojiPaint.textSize / 3, emojiPaint)
                if (animal.ready) {
                    emojiPaint.textSize = tileH * 0.5f
                    val prod = if (animal.type == "Cow") "🥛" else "🥚"
                    canvas.drawText(prod, cx + tileW / 4, cy - tileH / 4, emojiPaint)
                }
            }
            val charCol = charX.toInt()
            val charRow = charY.toInt()
            val (hcx, hcy) = tileCenter(charCol, charRow, tileW, tileH, topY)
            drawDiamond(canvas, hcx, hcy, tileW, tileH, highlightPaint)
            val charCx = width / 2f + (charX - charY) * tileW / 2
            val charCy = topY + (charX + charY) * tileH / 2
            charPaint.textSize = tileH * 1.4f
            canvas.drawText("🧑‍🌾", charCx, charCy + charPaint.textSize / 3, charPaint)
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
        listOf("Till", "Plant", "Water", "Harvest", "Clear", "Collect").forEach { name ->
            val b = Button(this).apply {
                text = name
                textSize = 12f
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

        val animalLabel = TextView(this).apply {
            text = "Animals (live in the pen, bottom right):"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(12), 0, dp(4))
        }
        root.addView(animalLabel)

        val animalRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val buyChicken = Button(this).apply {
            text = "Buy Chicken 50c"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { buyAnimal("Chicken", 50) }
        }
        val buyCow = Button(this).apply {
            text = "Buy Cow 150c"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { buyAnimal("Cow", 150) }
        }
        animalRow.addView(buyChicken)
        animalRow.addView(buyCow)
        root.addView(animalRow)

        farmView = FarmView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(320)
            )
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(farmView)

        val moveLabel = TextView(this).apply {
            text = "Isometric view: walk with the arrows. House is top left, pond top right, mine middle right, pen bottom right. Stand on a tile, then tap Use Tool."
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

        val homeButton = Button(this).apply {
            text = "🏠 Home"
            setOnClickListener {
                val c = charX.toInt()
                val r = charY.toInt()
                val nearHouse = (0..1).any { hc ->
                    (0..1).any { hr -> abs(c - hc) + abs(r - hr) <= 1 }
                }
                if (nearHouse) {
                    saveGame()
                    Toast.makeText(this@MainActivity, "Welcome home! Your game is saved.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Walk to your house, top left, first.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(homeButton)

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
                    .setMessage("This clears your farm, animals, coins, and day.")
                    .setPositiveButton("Yes, start over") { _, _ ->
                        prefs.edit().clear().apply()
                        coins = 50
                        day = 1
                        charX = 2.5f
                        charY = 1.5f
                        animals.clear()
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
            text = "How to play: Hold an arrow to walk. Field is the middle area, cols 2 to 7, rows 2 to 5. Till, plant, water, and harvest there. Buy a chicken for eggs (15c each) or a cow for milk (35c each). Animals make produce each new day, stand on one and use Collect. Pond and mine are on the map for fishing and mining in a later stage. Your farm, animals, and character position save automatically."
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

    private fun buyAnimal(type: String, cost: Int) {
        if (coins < cost) {
            Toast.makeText(this, "Not enough coins for a $type.", Toast.LENGTH_SHORT).show()
            return
        }
        val emptyPen = penTiles.firstOrNull { (c, r) ->
            animals.none { it.col == c && it.row == r }
        }
        if (emptyPen == null) {
            Toast.makeText(this, "Your pen is full.", Toast.LENGTH_SHORT).show()
            return
        }
        coins -= cost
        animals.add(Animal(type, emptyPen.first, emptyPen.second, false))
        saveGame()
        refreshAll()
        Toast.makeText(this, "Bought a $type! It will make produce tomorrow.", Toast.LENGTH_SHORT).show()
    }

    private fun useToolOnCurrentTile() {
        val c = charX.toInt()
        val r = charY.toInt()
        if (isHouseTile(c, r) || isPondTile(c, r) || isMineTile(c, r)) {
            Toast.makeText(this, "You cannot farm here.", Toast.LENGTH_SHORT).show()
            return
        }
        onTileClicked(currentTileIndex())
    }

    private fun onTileClicked(index: Int) {
        val c = index % cols
        val r = index / cols
        val tile = tiles[index]
        when (tool) {
            "Till", "Plant", "Water", "Harvest" -> {
                if (!isFieldTile(c, r)) {
                    Toast.makeText(this, "Farm in the middle field area.", Toast.LENGTH_SHORT).show()
                    saveGame()
                    refreshAll()
                    return
                }
            }
            "Collect" -> {
                val animal = animals.firstOrNull { it.col == c && it.row == r }
                if (animal == null) {
                    Toast.makeText(this, "No animal on this tile. Stand on a chicken or cow.", Toast.LENGTH_SHORT).show()
                } else if (!animal.ready) {
                    Toast.makeText(this, "Not ready yet. Come back tomorrow.", Toast.LENGTH_SHORT).show()
                } else {
                    val price = if (animal.type == "Cow") 35 else 15
                    val prod = if (animal.type == "Cow") "milk" else "an egg"
                    coins += price
                    animal.ready = false
                    Toast.makeText(this, "Collected $prod! +$price coins.", Toast.LENGTH_SHORT).show()
                }
                saveGame()
                refreshAll()
                return
            }
        }
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
        for (animal in animals) {
            animal.ready = true
            val emptyPen = penTiles.filter { (c, r) ->
                animals.none { it.col == c && it.row == r } || (animal.col == c && animal.row == r)
            }
            if (emptyPen.isNotEmpty()) {
                val dest = emptyPen[Random.nextInt(emptyPen.size)]
                animal.col = dest.first
                animal.row = dest.second
            }
        }
        day += 1
        saveGame()
        refreshAll()
        if (grew > 0) {
            Toast.makeText(this, "Day $day! $grew crops grew, and your animals made produce.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Day $day! Your animals made produce.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tileEmoji(tile: Tile): String {
        if (tile.state == 0) return ""
        if (tile.state == 1) return ""
        val crop = crops[tile.crop] ?: return "🌱"
        return when {
            tile.progress >= crop.growthDays -> crop.readyEmoji
            tile.progress == 0 -> "🌱"
            else -> "🌿"
        }
    }

    private fun refreshAll() {
        val animalCount = animals.size
        statusText.text = "Day $day  •  🪙 $coins coins  •  🐔🐄 $animalCount animals"
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
        val animalString = animals.joinToString(";") {
            "${it.type},${it.col},${it.row},${if (it.ready) 1 else 0}"
        }
        prefs.edit()
            .putInt("coins", coins)
            .putInt("day", day)
            .putString("tiles", tileString)
            .putString("animals", animalString)
            .putFloat("charX", charX)
            .putFloat("charY", charY)
            .apply()
    }

    private fun loadGame() {
        coins = prefs.getInt("coins", 50)
        day = prefs.getInt("day", 1)
        charX = prefs.getFloat("charX", 2.5f)
        charY = prefs.getFloat("charY", 1.5f)
        val saved = prefs.getString("tiles", null)
        if (saved != null) {
            val parts = saved.split(";")
            if (parts.size == tiles.size) {
                for (i in parts.indices) {
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
        val savedAnimals = prefs.getString("animals", null)
        if (!savedAnimals.isNullOrEmpty()) {
            for (part in savedAnimals.split(";")) {
                val f = part.split(",")
                if (f.size < 4) continue
                animals.add(
                    Animal(
                        type = f[0],
                        col = f[1].toIntOrNull() ?: 8,
                        row = f[2].toIntOrNull() ?: 5,
                        ready = f[3] == "1"
                    )
                )
            }
        }
    }
}

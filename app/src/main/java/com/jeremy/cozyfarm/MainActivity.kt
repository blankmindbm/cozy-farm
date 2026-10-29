package com.jeremy.cozyfarm

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

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

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var toolText: TextView
    private val tileButtons = mutableListOf<Button>()
    private val toolButtons = mutableMapOf<String, Button>()
    private val seedButtons = mutableMapOf<String, Button>()

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

        for (r in 0 until rows) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0 until cols) {
                val index = r * cols + c
                val b = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(58), 1f)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    setPadding(0, 0, 0, 0)
                    setOnClickListener { onTileClicked(index) }
                }
                tileButtons.add(b)
                rowLayout.addView(b)
            }
            root.addView(rowLayout)
        }

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
            text = "How to play: 1. Till the grass. 2. Pick a seed and Plant it. 3. Water it. 4. Tap Next Day to grow it. Water again each day or it will not grow. 5. Harvest when you see 🌾, 🥕, or 🎃. Wheat takes 2 days, Carrot 3, Pumpkin 4. Your farm saves automatically."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(help)

        setContentView(scroll)
        refreshAll()
    }

    private fun dp(v: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
        ).toInt()
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
        if (tile.state == 1) return "🟤"
        val crop = crops[tile.crop] ?: return "🌱"
        val base = when {
            tile.progress >= crop.growthDays -> crop.readyEmoji
            tile.progress == 0 -> "🌱"
            else -> "🌿"
        }
        return if (tile.watered) "$base 💧" else base
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
        for (i in tiles.indices) {
            tileButtons[i].text = tileEmoji(tiles[i])
        }
    }

    private fun saveGame() {
        val tileString = tiles.joinToString(";") {
            "${it.state},${it.crop},${it.progress},${if (it.watered) 1 else 0}"
        }
        prefs.edit()
            .putInt("coins", coins)
            .putInt("day", day)
            .putString("tiles", tileString)
            .apply()
    }

    private fun loadGame() {
        coins = prefs.getInt("coins", 50)
        day = prefs.getInt("day", 1)
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

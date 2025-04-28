package com.dardi.bikeracing

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var playerBike: ImageView
    private lateinit var roadBackground1: ImageView
    private lateinit var roadBackground2: ImageView
    private lateinit var scoreText: TextView
    private lateinit var restartButton: Button
    private var score = 0
    private val baseRoadSpeed = 15
    private var dynamicRoadSpeed = baseRoadSpeed
    private val handler = Handler()
    private var gameRunning = true
    private val enemyBikes = mutableListOf<ImageView>()
    private val enemySpeeds = mutableListOf<Int>() // Individual speeds for each enemy
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize views
        playerBike = findViewById(R.id.playerBike)
        roadBackground1 = findViewById(R.id.roadBackground1)
        roadBackground2 = findViewById(R.id.roadBackground2)
        scoreText = findViewById(R.id.scoreText)
        restartButton = findViewById(R.id.restartButton)

        playerBike.post {
            playerBike.x = (resources.displayMetrics.widthPixels / 2 - playerBike.width / 2).toFloat()
        }
        // Start game
        startNewGame()
        // Restart game on button click
        restartButton.setOnClickListener {
            startNewGame()
        }
    }
    private fun startNewGame() {
        // Reset variables
        score = 0
        gameRunning = true
        scoreText.text = "Score: $score"
        restartButton.visibility = Button.GONE
        // Reset road speed
        dynamicRoadSpeed = baseRoadSpeed
        // Remove old enemy bikes from the layout and clear the lists
        val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
        for (enemyBike in enemyBikes) {
            rootLayout.removeView(enemyBike)
        }
        enemyBikes.clear()
        enemySpeeds.clear()
        // Reset road positions
        roadBackground1.y = 0f
        roadBackground2.y = -resources.displayMetrics.heightPixels.toFloat()
        // Create and add new enemy bikes dynamically
        val numberOfEnemies = 3 // You can increase this number to add more enemies
        for (i in 1..numberOfEnemies) {
            val enemyBike = ImageView(this).apply {
                setImageResource(R.drawable.bikeblue)
                layoutParams = RelativeLayout.LayoutParams(
                    140, // Enemy width
                    200  // Enemy height
                ).apply {
                    topMargin = -200 // Start above the screen
                }
                x = Random.nextInt(
                    0,
                    resources.displayMetrics.widthPixels - 140 // Adjusted for new width
                ).toFloat()
                y = -200f
            }
            rootLayout.addView(enemyBike) // Add to layout
            enemyBikes.add(enemyBike)
            enemySpeeds.add(10 + i * 2) // Varying speed for each enemy
        }
        playerBike.post {
            playerBike.x = (resources.displayMetrics.widthPixels / 2 - playerBike.width / 2).toFloat()
        }
        // Start mechanics
        setupScrollingBackground()
        moveEnemyBikes()
        setupPlayerControls()
    }
    private fun setupScrollingBackground() {
        val runnable = object : Runnable {
            override fun run() {
                if (!gameRunning) return

                // Move backgrounds at dynamic speed
                roadBackground1.y += dynamicRoadSpeed
                roadBackground2.y += dynamicRoadSpeed

                // Reset positions
                if (roadBackground1.y >= resources.displayMetrics.heightPixels) {
                    roadBackground1.y = roadBackground2.y - resources.displayMetrics.heightPixels
                }
                if (roadBackground2.y >= resources.displayMetrics.heightPixels) {
                    roadBackground2.y = roadBackground1.y - resources.displayMetrics.heightPixels
                }

                handler.postDelayed(this, 30)
            }
        }
        handler.post(runnable)
    }
    private fun moveEnemyBikes() {
        val runnable = object : Runnable {
            override fun run() {
                if (!gameRunning) return

                for (i in enemyBikes.indices) {
                    val enemyBike = enemyBikes[i]
                    val newY = enemyBike.y + enemySpeeds[i]

                    if (newY > resources.displayMetrics.heightPixels) {
                        // Reset enemy bike position
                        enemyBike.y = -200f
                        enemyBike.x = Random.nextInt(
                            0,
                            resources.displayMetrics.widthPixels - 140
                        ).toFloat()

                        // Increment score
                        score++
                        scoreText.text = "Score: $score"

                        // Adjust speed dynamically based on score
                        if (score % 1 == 0) { // Change speed every 5 points
                            val speedIncrement = (1 + score / 20).coerceAtMost(10) // Gradual increment
                            enemySpeeds[i] += speedIncrement
                            dynamicRoadSpeed += speedIncrement  // Increase road speed slower
                        }
                    } else {
                        enemyBike.y = newY
                    }
                }
                checkCollisions()
                handler.postDelayed(this, 30)
            }
        }
        handler.post(runnable)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPlayerControls() {
        val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)

        rootLayout.setOnTouchListener { _, event ->
            if (!gameRunning) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    // Adjust the position of the bike based on the touch
                    val offsetY = 200f // Offset the bike above the touch point

                    // Calculate the new x and y positions
                    var newX = event.rawX - playerBike.width / 2
                    var newY = event.rawY - playerBike.height / 2 - offsetY

                    // Clamp the position to keep the bike within screen boundaries
                    val screenWidth = resources.displayMetrics.widthPixels
                    val screenHeight = resources.displayMetrics.heightPixels
                    val minX = 0f
                    val maxX = (screenWidth - playerBike.width).toFloat()
                    val minY = 0f
                    val maxY = (screenHeight - playerBike.height).toFloat()

                    newX = newX.coerceIn(minX, maxX)
                    newY = newY.coerceIn(minY, maxY)

                    // Update the bike's position
                    playerBike.x = newX
                    playerBike.y = newY
                }
            }
            true
        }
    }

    private fun checkCollisions() {
        val padding = 30 // Adjust collision area

        val playerRect = Rect(
            (playerBike.x + padding).toInt(),
            (playerBike.y + padding).toInt(),
            (playerBike.x + playerBike.width - padding).toInt(),
            (playerBike.y + playerBike.height - padding).toInt()
        )

        for (enemyBike in enemyBikes) {
            val enemyRect = Rect(
                (enemyBike.x + padding).toInt(),
                (enemyBike.y + padding).toInt(),
                (enemyBike.x + enemyBike.width - padding).toInt(),
                (enemyBike.y + enemyBike.height - padding).toInt()
            )

            if (Rect.intersects(playerRect, enemyRect)) {
                endGame()
                break
            }
        }
    }

    private fun endGame() {
        gameRunning = false
        handler.removeCallbacksAndMessages(null)
        scoreText.text = "Game Over! Score: $score"
        restartButton.visibility = Button.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
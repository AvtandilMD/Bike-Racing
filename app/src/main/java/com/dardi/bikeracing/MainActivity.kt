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
    private val enemySpeeds = mutableListOf<Int>()

    // Smooth movement variables
    private var targetX = 0f // სადაც უნდა მივიდეს ბაიკი
    private var currentX = 0f // ამჟამინდელი პოზიცია
    private val movementSpeed = 0.05f // მოძრაობის სიჩქარე (0.1 = ნელა, 0.3 = სწრაფად)

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
            val screenWidth = resources.displayMetrics.widthPixels
            currentX = (screenWidth / 2 - playerBike.width / 2).toFloat()
            targetX = currentX
            playerBike.x = currentX
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

        val screenWidth = resources.displayMetrics.widthPixels
        val roadLeftMargin = (screenWidth * 0.15).toInt()
        val roadRightMargin = (screenWidth * 0.15).toInt()
        val roadWidth = screenWidth - roadLeftMargin - roadRightMargin
        val enemyWidth = 140

        val numberOfEnemies = 3
        for (i in 1..numberOfEnemies) {
            val enemyBike = ImageView(this).apply {
                setImageResource(R.drawable.bikeblue)
                layoutParams = RelativeLayout.LayoutParams(
                    enemyWidth,
                    200
                ).apply {
                    topMargin = -200
                }
                x = (roadLeftMargin + Random.nextInt(
                    0,
                    roadWidth - enemyWidth
                )).toFloat()
                y = -200f
            }
            rootLayout.addView(enemyBike)
            enemyBikes.add(enemyBike)
            enemySpeeds.add(10 + i * 2)
        }

        playerBike.post {
            currentX = (screenWidth / 2 - playerBike.width / 2).toFloat()
            targetX = currentX
            playerBike.x = currentX
            playerBike.y = (resources.displayMetrics.heightPixels * 0.8).toFloat()
        }

        // Start mechanics
        setupScrollingBackground()
        moveEnemyBikes()
        setupPlayerControls()
        smoothPlayerMovement() // ახალი ფუნქცია smooth movement-ისთვის
    }

    private fun setupScrollingBackground() {
        val runnable = object : Runnable {
            override fun run() {
                if (!gameRunning) return

                roadBackground1.y += dynamicRoadSpeed
                roadBackground2.y += dynamicRoadSpeed

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

                val screenWidth = resources.displayMetrics.widthPixels
                val roadLeftMargin = (screenWidth * 0.15).toInt()
                val roadRightMargin = (screenWidth * 0.15).toInt()
                val roadWidth = screenWidth - roadLeftMargin - roadRightMargin
                val enemyWidth = 140

                for (i in enemyBikes.indices) {
                    val enemyBike = enemyBikes[i]
                    val newY = enemyBike.y + enemySpeeds[i]

                    if (newY > resources.displayMetrics.heightPixels) {
                        enemyBike.y = -200f
                        enemyBike.x = (roadLeftMargin + Random.nextInt(
                            0,
                            roadWidth - enemyWidth
                        )).toFloat()

                        score++
                        scoreText.text = "Score: $score"

                        if (score % 1 == 0) {
                            val speedIncrement = (1 + score / 20).coerceAtMost(10)
                            enemySpeeds[i] += speedIncrement
                            dynamicRoadSpeed += speedIncrement
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

    // ახალი ფუნქცია smooth movement-ისთვის
    private fun smoothPlayerMovement() {
        val runnable = object : Runnable {
            override fun run() {
                if (!gameRunning) return

                // თანდათან მიახლოება targetX-ს
                val distance = targetX - currentX
                if (kotlin.math.abs(distance) > 1f) { // თუ დისტანცია 1 პიქსელზე მეტია
                    currentX += distance * movementSpeed
                    playerBike.x = currentX
                }

                handler.postDelayed(this, 16) // ~60 FPS
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
                    val screenWidth = resources.displayMetrics.widthPixels
                    val roadLeftMargin = (screenWidth * 0.15).toFloat()
                    val roadWidth = screenWidth - (roadLeftMargin * 2)

                    var touchX = event.x
                    var newTargetX = touchX - playerBike.width / 2

                    val minX = roadLeftMargin
                    val maxX = roadLeftMargin + roadWidth - playerBike.width

                    // მხოლოდ target position-ს ვაყენებთ, actual movement იქნება smooth
                    targetX = newTargetX.coerceIn(minX, maxX)
                }
            }
            true
        }
    }

    private fun checkCollisions() {
        val padding = 30

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
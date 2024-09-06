package com.broccolistefanipss.sportstracker.activity

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.broccolistefanipss.sportstracker.R
import com.broccolistefanipss.sportstracker.databinding.ActivityNewTrainingBinding
import com.broccolistefanipss.sportstracker.global.DB
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class NewTrainingActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityNewTrainingBinding
    private lateinit var sensorManager: SensorManager

    private var accelerometer: Sensor? = null

    private var isRunning = false
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L

    private var totalAcceleration: Double = 0.0
    private var calorieCount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewTrainingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        binding.startStopButton.setOnClickListener {
            if (!isRunning) {
                startTraining()
            } else {
                stopTraining()
            }
        }

        binding.closeButton.setOnClickListener {
            closeTraining()
        }

        // Inizialmente disabilita il pulsante di chiusura
        binding.closeButton.isEnabled = false
    }

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            elapsedTime = updateTimer()
            timerHandler.postDelayed(this, 1000)
        }
    }

    private fun formatDuration(durationInMillis: Long): String {
        val hours = durationInMillis / 3600000
        val minutes = (durationInMillis % 3600000) / 60000
        val seconds = (durationInMillis % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun startTimer() {
        resetTrainingData()
        timerHandler.postDelayed(timerRunnable, 0)
    }

    private fun stopTimer() {
        if (isRunning) {
            val currentTime = System.currentTimeMillis()
            elapsedTime += currentTime - startTime
        }
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimer(): Long {
        if (isRunning) {
            val currentTime = System.currentTimeMillis()
            val newElapsedTime = currentTime - startTime + 1

            binding.timeTextView.text = formatDuration(newElapsedTime)
            binding.calorieTextView.text = String.format(Locale.getDefault(), "Calories: %.2f", calorieCount)
            return newElapsedTime
        }
        return 0
    }

    private fun startAccelerometer() {
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopAccelerometer() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isRunning && event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val accelerationMagnitude = sqrt((x * x + y * y + z * z).toDouble())
            totalAcceleration += accelerationMagnitude

            calorieCount = totalAcceleration * 0.01
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non implementato
    }

    private fun saveTrainingSession() {
        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)

        val userName = sharedPreferences.getString("userName", null)

        val sessionDate = getCurrentDate()
        val durationInSeconds = (elapsedTime / 1000).toInt()

        Log.d("NewTrainingActivity", "Elapsed time in seconds: $durationInSeconds")

        val trainingType = "corsa"
        val burntCalories = calorieCount.toInt()

        val db = DB(this)
        if (userName != null) {
            val sessionId = db.insertTrainingSession(userName, sessionDate, durationInSeconds, trainingType, burntCalories)
            Log.d("NewTrainingActivity", "Session ID: $sessionId, Salvataggio sessione in corso: $userName, $sessionDate, $durationInSeconds, $trainingType, $burntCalories")
        } else {
            Log.e("NewTrainingActivity", "Errore: userName non trovato")
        }
    }

    private fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun startTraining() {
        isRunning = true
        binding.startStopButton.text = getString(R.string.stop)
        startTime = System.currentTimeMillis()
        startTimer()
        startAccelerometer()
        binding.closeButton.isEnabled = false
        updateButtonColor()
    }

    private fun updateButtonColor() {
        if (binding.closeButton.isEnabled) {
            binding.closeButton.setBackgroundResource(R.drawable.button_style) // colore del bottone normale
        } else {
            binding.closeButton.setBackgroundResource(R.drawable.button_style_disabled) // colore del bottone disabilitato
        }
    }
    private fun stopTraining() {
        isRunning = false
        binding.startStopButton.text = getString(R.string.start)
        stopTimer()
        stopAccelerometer()
        saveTrainingSession()
        binding.closeButton.isEnabled = true
        updateButtonColor()
    }

    private fun closeTraining() {
        if (isRunning) {
            stopTraining()
        }
        resetTrainingData()
        finish()
    }

    private fun resetTrainingData() {
        totalAcceleration = 0.0
        calorieCount = 0.0
        elapsedTime = 0L
        binding.timeTextView.text = getString(R.string.reset_time)
        binding.calorieTextView.text = getString(R.string.reset_calories)
    }
}
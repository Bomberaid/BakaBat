package com.example.bakabat

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    lateinit var mSensorManager: SensorManager
    lateinit var mAccelerometer: Sensor
    lateinit var mShakeDetector: ShakeDetector

    //lateinit var textview: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ShakeDetector initialization
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mShakeDetector = ShakeDetector()

        val textview: TextView = findViewById(R.id.Center)

        var lowBaka = MediaPlayer.create(applicationContext, R.raw.lowbaka)
        var loudBaka = MediaPlayer.create(applicationContext, R.raw.loudbaka)
        var DIO = MediaPlayer.create(applicationContext, R.raw.dio)

        mShakeDetector.setOnShakeListener(object: ShakeDetector.OnShakeListener {

            override fun onShake(count: Int) {
                // TODO: Play Baka sound.
                if (mShakeDetector.fNet >= 10) {
                    DIO.start()
                }
                /*else if (mShakeDetector.gForce > 5) {
                    loudBaka.start()
                }*/
                else if (mShakeDetector.fNet > mShakeDetector.FORCE_THRESHOLD) {
                    lowBaka.start()
                }
                var text: String = mShakeDetector.fNet.toString() + " N"

                textview.text = text
                Log.d("Shake", "Shaken!")
            }
        })
    }

    override fun onResume() {
        super.onResume()

        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector)

        super.onPause()
    }

    /*fun changeCenterText(newText: String) {
        if (textview != null) {
            textview.text = newText
        }
    }*/
}

class ShakeDetector: SensorEventListener {
    val FORCE_THRESHOLD = 3f
    private val SHAKE_SLOP_TIME_MS = 500
    private val SHAKE_COUNT_RESET_TIME_MS = 3000

    private lateinit var mListener: OnShakeListener
    private var mShakeTimeStamp: Long = 0
    private var mShakeCount: Int = 0

    private val PHONE_MASS = 0.180f                  // The average weight of a smartphone in kg.

    //var gForce: Float = 0f
    var fNet: Float = 0f

    fun setOnShakeListener(listener: OnShakeListener) {
        this.mListener = listener
    }

    interface OnShakeListener {
        fun onShake(count: Int)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // ignore.
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (mListener != null) {

            var x: Float = event.values[0]
            var y: Float = event.values[1]
            var z: Float = event.values[2]

            val alpha = 0.6f;
            var gravity = FloatArray(3)
            var linear_acceleration = FloatArray(3)

            // Low Pass filter to remove gravity from calculated accelerations
            /*gravity[0] = alpha * SensorManager.GRAVITY_EARTH + (1 - alpha) * x;
            gravity[1] = alpha * SensorManager.GRAVITY_EARTH + (1 - alpha) * y;
            gravity[2] = alpha * SensorManager.GRAVITY_EARTH + (1 - alpha) * z;*/

            linear_acceleration[0] = x //- gravity[0];
            linear_acceleration[1] = y //- gravity[1];
            linear_acceleration[2] = z //- gravity[2];

            // Net Acceleration is just a1 + a2 + a3 (not sqrt(a1^2 + a2^2 + a3^2))
            var netAcceleration: Float = sqrt(linear_acceleration[0].pow(2) + linear_acceleration[1].pow(2) + linear_acceleration[2].pow(2)) - SensorManager.GRAVITY_EARTH

            Log.d("Net Acceleration", netAcceleration.toString())

            // Calulate the amount of newtons phone is experiencing using formula Fnet = ma
            fNet = PHONE_MASS * netAcceleration

            if (fNet > FORCE_THRESHOLD) {
                val now: Long = System.currentTimeMillis()

                // Ignore shake events too close to each other (500ms).
                if (mShakeTimeStamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                // Reset the shake count after 3 seconds
                if (mShakeTimeStamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0
                }

                mShakeTimeStamp = now
                mShakeCount++

                mListener.onShake(mShakeCount)
            }


        }
    }
}

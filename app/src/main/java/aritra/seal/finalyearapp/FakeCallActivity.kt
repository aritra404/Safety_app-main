package aritra.seal.finalyearapp

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FakeCallActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var callerNameTextView: TextView

    companion object {
        const val EXTRA_CALLER_NAME = "extra_caller_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set flags to show the activity over the lock screen and turn on the screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        setContentView(R.layout.activity_fake_call)

        callerNameTextView = findViewById(R.id.callerNameTextView)
        val acceptButton: ImageView = findViewById(R.id.acceptCallButton)
        val declineButton: ImageView = findViewById(R.id.declineCallButton)

        // Get caller name from intent, default to "Unknown"
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown"
        callerNameTextView.text = callerName

        startRingingAndVibrating()

        acceptButton.setOnClickListener {
            handleCallAccepted()
        }

        declineButton.setOnClickListener {
            handleCallDeclined()
        }
    }

    private fun startRingingAndVibrating() {
        // Play default ringtone
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer.create(this, notificationUri)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not play ringtone. Ensure a default ringtone is set.", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }

        // Vibrate
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 1000, 1000) // Start immediately, vibrate for 1s, pause for 1s
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // Loop indefinitely
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0) // Loop indefinitely
            }
        }
    }

    private fun stopRingingAndVibrating() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
    }

    private fun handleCallAccepted() {
        stopRingingAndVibrating()
        Toast.makeText(this, "Fake call answered! Talking to ${callerNameTextView.text}...", Toast.LENGTH_SHORT).show()
        // Simulate a short conversation duration
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Fake call ended.", Toast.LENGTH_SHORT).show()
            finish() // Finish the activity
        }, 3000) // Simulate a 3-second call
    }

    private fun handleCallDeclined() {
        stopRingingAndVibrating()
        Toast.makeText(this, "Fake call declined.", Toast.LENGTH_SHORT).show()
        finish() // Finish the activity
    }

    override fun onBackPressed() {
        // Treat back press as declining the call
        handleCallDeclined()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingingAndVibrating() // Ensure resources are released
    }
}

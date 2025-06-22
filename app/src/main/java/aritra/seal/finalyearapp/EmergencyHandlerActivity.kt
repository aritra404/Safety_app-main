package aritra.seal.finalyearapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class EmergencyHandlerActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var guardianNumber: String? = null
    private lateinit var statusText: TextView
    private lateinit var cancelButton: Button
    private var emergencyInProgress = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_handler)

        // Make activity appear over lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize UI elements
        statusText = findViewById(R.id.emergencyStatusText)
        cancelButton = findViewById(R.id.cancelEmergencyButton)

        // Get data from intent
        guardianNumber = intent.getStringExtra("GUARDIAN_NUMBER")
        val detectionWord = intent.getStringExtra("DETECTION_WORD") ?: "help"

        Log.i("EmergencyHandler", "Activity started with guardian: $guardianNumber")

        // Setup initial UI
        updateStatus("🚨 EMERGENCY DETECTED! 🚨")
        updateStatus("Detection word: '${detectionWord.uppercase()}'")
        updateStatus("Guardian: $guardianNumber")
        updateStatus("")
        updateStatus("Actions in progress:")

        // Setup cancel button
        cancelButton.setOnClickListener {
            if (emergencyInProgress) {
                Log.i("EmergencyHandler", "Emergency cancelled by user")
                emergencyInProgress = false
                updateStatus("❌ Emergency cancelled by user")
                Toast.makeText(this, "Emergency cancelled", Toast.LENGTH_LONG).show()

                // Delay finish to show cancellation message
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 2000)
            }
        }

        // Handle the emergency if guardian number is available
        if (!guardianNumber.isNullOrEmpty()) {
            startEmergencySequence(guardianNumber!!)
        } else {
            updateStatus("❌ ERROR: Guardian number not set!")
            Toast.makeText(this, "Guardian number not configured!", Toast.LENGTH_LONG).show()
        }
    }

    private fun startEmergencySequence(phoneNumber: String) {
        if (!emergencyInProgress) return

        Log.i("EmergencyHandler", "Starting emergency sequence")

        // Step 1: Send SMS immediately
        updateStatus("📱 Sending emergency SMS...")
        sendEmergencySMS(phoneNumber)

        // Step 2: Show recording status
        updateStatus("🎤 Audio recording in progress...")

        // Step 3: Simulate the 20-second recording duration
        Handler(Looper.getMainLooper()).postDelayed({
            if (emergencyInProgress) {
                updateStatus("📤 Uploading audio recording...")

                // Step 4: Make emergency call after a brief delay
                Handler(Looper.getMainLooper()).postDelayed({
                    if (emergencyInProgress) {
                        updateStatus("📞 Making emergency call...")
                        makeEmergencyCall(phoneNumber)

                        // Step 5: Show completion status
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (emergencyInProgress) {
                                updateStatus("✅ Emergency sequence completed!")
                                updateStatus("Guardian has been notified.")

                                // Auto-close after showing completion
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (!isFinishing) {
                                        finish()
                                    }
                                }, 5000)
                            }
                        }, 2000)
                    }
                }, 3000)
            }
        }, 20000) // 20 seconds for recording
    }

    private fun sendEmergencySMS(phoneNumber: String) {
        if (!emergencyInProgress) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("EmergencyHandler", "SEND_SMS permission not granted")
            updateStatus("❌ SMS permission not granted")
            return
        }

        try {
            val message = "EMERGENCY: I need help! This is an urgent alert from my safety app."
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            Log.i("EmergencyHandler", "Emergency SMS sent successfully")
            updateStatus("✅ Emergency SMS sent")
            Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_SHORT).show()

            // Send location in separate SMS
            getCurrentLocation { location ->
                if (location != null && emergencyInProgress) {
                    sendLocationSMS(phoneNumber, location)
                }
            }

        } catch (e: Exception) {
            Log.e("EmergencyHandler", "Failed to send emergency SMS: ${e.message}")
            updateStatus("❌ Failed to send SMS: ${e.message}")
        }
    }

    private fun sendLocationSMS(phoneNumber: String, location: Location) {
        if (!emergencyInProgress) return

        try {
            val locationUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            val locationMessage = "My current location: $locationUrl"

            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, locationMessage, null, null)

            Log.i("EmergencyHandler", "Location SMS sent")
            updateStatus("✅ Location sent to guardian")

        } catch (e: Exception) {
            Log.e("EmergencyHandler", "Location SMS failed: ${e.message}")
            updateStatus("❌ Failed to send location")
        }
    }

    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("EmergencyHandler", "Location permission not granted")
            updateStatus("❌ Location permission not granted")
            callback(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.i("EmergencyHandler", "Location obtained: ${location.latitude}, ${location.longitude}")
                } else {
                    Log.w("EmergencyHandler", "Location is null")
                }
                callback(location)
            }
            .addOnFailureListener { e ->
                Log.e("EmergencyHandler", "Failed to get location: ${e.message}")
                updateStatus("❌ Failed to get location")
                callback(null)
            }
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        if (!emergencyInProgress) return

        Log.i("EmergencyHandler", "Attempting to make emergency call to: $phoneNumber")

        try {
            // Check if CALL_PHONE permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                Log.e("EmergencyHandler", "CALL_PHONE permission not granted")
                updateStatus("❌ Call permission not granted. Opening dialer...")

                // Fallback to dialer
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(dialIntent)
                updateStatus("📞 Please tap call button to contact guardian")
                Toast.makeText(this, "Please tap call to contact guardian", Toast.LENGTH_LONG).show()
                return
            }

            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivity(callIntent)
            Log.i("EmergencyHandler", "Emergency call initiated successfully")
            updateStatus("✅ Calling guardian: $phoneNumber")
            Toast.makeText(this, "Calling guardian", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("EmergencyHandler", "Emergency call failed: ${e.message}")
            updateStatus("❌ Call failed. Trying dialer...")

            // Fallback: Open dialer
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(dialIntent)
                updateStatus("📞 Please tap call to contact guardian")
                Toast.makeText(this, "Please tap call to contact guardian", Toast.LENGTH_LONG).show()

            } catch (dialException: Exception) {
                Log.e("EmergencyHandler", "Dialer fallback failed: ${dialException.message}")
                updateStatus("❌ Unable to make call. Check permissions.")
            }
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            val currentText = statusText.text.toString()
            statusText.text = if (currentText.isEmpty()) {
                message
            } else {
                "$currentText\n$message"
            }
            Log.i("EmergencyHandler", "Status: $message")
        }
    }

    override fun onBackPressed() {
        // Prevent back button from closing during emergency
        if (emergencyInProgress) {
            Toast.makeText(this, "Emergency in progress. Use Cancel button to stop.", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        emergencyInProgress = false
        Log.i("EmergencyHandler", "Emergency handler activity destroyed")
    }
}
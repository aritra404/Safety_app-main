package aritra.seal.finalyearapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View // Import View for setOnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout // Import ConstraintLayout for finding views
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class MainActivity2 : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var statusTextView: TextView
    private lateinit var guardianNumberTextView: TextView
    private lateinit var detectionWordEditText: EditText
    private lateinit var activeSwitch: SwitchMaterial
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // New for quick tools
    private lateinit var locationSharingTool: ConstraintLayout
    private lateinit var fakeCallTool: ConstraintLayout
    private lateinit var Chattool: ConstraintLayout
    private var mediaPlayer: MediaPlayer? = null // For fake call audio

    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION, // Combines fine and coarse
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.INTERNET
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        statusTextView = findViewById(R.id.statusTextView)
        guardianNumberTextView = findViewById(R.id.guardianNumberTextView)
        detectionWordEditText = findViewById(R.id.detectionWordTextView)
        activeSwitch = findViewById(R.id.activeSwitch)
        val setGuardianButton: Button = findViewById(R.id.setGuardianButton)
        val setDetectionWordButton: Button = findViewById(R.id.setTriggerWordButton)

        // Initialize quick tool views
        locationSharingTool = findViewById(R.id.locationSharingTool)
        fakeCallTool = findViewById(R.id.fakeCallTool)
        Chattool = findViewById(R.id.ChatTool)


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE
            )
        }

        // Initialize speech recognizer for the activity
        setupSpeechRecognizer()

        // Setup switch listener
        activeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val detectionWord = getCurrentDetectionWord()
            if (isChecked) {
                if (detectionWord.isNotEmpty()) {
                    startHelpDetectionService()
                    statusTextView.text = "Listening for '$detectionWord'... (Running in background)"
                } else {
                    Toast.makeText(this, "Please set a detection word first", Toast.LENGTH_SHORT).show()
                    activeSwitch.isChecked = false
                }
            } else {
                stopHelpDetectionService()
                stopListening()
                statusTextView.text = "Detection stopped"
            }
        }

        // Setup guardian number button
        setGuardianButton.setOnClickListener {
            val intent = Intent(this, GuardianSettingsActivity::class.java)
            startActivity(intent)
        }

        // Setup detection word button
        setDetectionWordButton.setOnClickListener {
            val newWord = detectionWordEditText.text.toString().trim().lowercase()
            if (newWord.isNotEmpty()) {
                val sharedPref = getSharedPreferences("HelpDetectorPrefs", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("detectionWord", newWord)
                    apply()
                }

                detectionWordEditText.setText(newWord)
                Toast.makeText(this, "Detection word set to: $newWord", Toast.LENGTH_SHORT).show()

                if (activeSwitch.isChecked) {
                    stopHelpDetectionService()
                    startHelpDetectionService()
                    statusTextView.text = "Listening for '$newWord'... (Running in background)"
                }
            } else {
                Toast.makeText(this, "Please enter a valid word", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup Quick Tool Listeners
        locationSharingTool.setOnClickListener {
            shareCurrentLocation()
        }

        Chattool.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        fakeCallTool.setOnClickListener {
            simulateFakeCall()
        }

        // Load data from shared preferences
        loadPreferences()

        // Check if service is running and update switch accordingly
        updateSwitchState()
    }

    private fun loadPreferences() {
        val sharedPref = getSharedPreferences("HelpDetectorPrefs", MODE_PRIVATE)
        val guardianNumber = sharedPref.getString("guardianNumber", "")
        val detectionWord = sharedPref.getString("detectionWord", "help")

        guardianNumberTextView.text = guardianNumber?.ifEmpty { "Not set" }
        detectionWordEditText.setText(detectionWord)
    }

    private fun getCurrentDetectionWord(): String {
        val sharedPref = getSharedPreferences("HelpDetectorPrefs", MODE_PRIVATE)
        return sharedPref.getString("detectionWord", "help") ?: "help"
    }

    private fun updateSwitchState() {
        val serviceRunning = isServiceRunning(HelpDetectionService::class.java)
        val detectionWord = getCurrentDetectionWord()
        activeSwitch.isChecked = serviceRunning
        if (serviceRunning) {
            statusTextView.text = "Listening for '$detectionWord'... (Running in background)"
        } else {
            statusTextView.text = "Detection stopped"
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startHelpDetectionService() {
        if (!allPermissionsGranted()) {
            Toast.makeText(this, "Required permissions not granted to start service.", Toast.LENGTH_SHORT).show()
            activeSwitch.isChecked = false
            return
        }
        val serviceIntent = Intent(this, HelpDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopHelpDetectionService() {
        val serviceIntent = Intent(this, HelpDetectionService::class.java)
        stopService(serviceIntent)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (!isServiceRunning(HelpDetectionService::class.java) && activeSwitch.isChecked) {
                    startListening()
                }
            }

            override fun onError(error: Int) {
                if (!isServiceRunning(HelpDetectionService::class.java) && activeSwitch.isChecked) {
                    startListening()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val detectionWord = getCurrentDetectionWord()

                if (matches != null) {
                    for (result in matches) {
                        if (result.lowercase().contains(detectionWord.lowercase())) {
                            Toast.makeText(this@MainActivity2, "${detectionWord.uppercase()} detected!", Toast.LENGTH_LONG).show()
                            statusTextView.text = "Detected '${detectionWord.uppercase()}'! Service handling..."
                            break
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val detectionWord = getCurrentDetectionWord()
                if (matches != null && matches.isNotEmpty()) {
                    val currentPartial = matches[0]
                    if (currentPartial.lowercase().contains(detectionWord.lowercase())) {
                        // Optional: show a small toast for partial match
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (allPermissionsGranted() && !isServiceRunning(HelpDetectionService::class.java) && activeSwitch.isChecked) {
            speechRecognizer.startListening(recognizerIntent)
        } else {
            speechRecognizer.cancel()
        }
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                if (activeSwitch.isChecked) {
                    startHelpDetectionService()
                }
            } else {
                Toast.makeText(this, "Required permissions not granted. Functionality limited.", Toast.LENGTH_LONG).show()
                activeSwitch.isChecked = false
            }
        }
    }

    // --- New Functionalities for Quick Tools ---

    @SuppressLint("MissingPermission") // Suppress lint for check as permissions are handled at runtime
    private fun shareCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val mapUrl = "https://maps.google.com/maps?q=$latitude,$longitude"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "I am at this location: $mapUrl")
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share location via"))
                } else {
                    Toast.makeText(this, "Could not get current location. Make sure GPS is enabled.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun simulateFakeCall() {
        Toast.makeText(this, "Simulating incoming call...", Toast.LENGTH_SHORT).show()

        // 1. Play a ringtone sound
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.ringtone) // You need to create a raw/ringtone.mp3 or .ogg file
            mediaPlayer?.start()
            mediaPlayer?.isLooping = true // Loop the ringtone
        } catch (e: Exception) {
            Toast.makeText(this, "Could not play ringtone. Add 'ringtone.mp3' to res/raw folder.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            return // Don't proceed if audio fails
        }

        // 2. Show a dialog after a delay to simulate picking up/ending call
        Handler(Looper.getMainLooper()).postDelayed({
            // Stop the ringtone
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

        }, 5000) // Simulate ringing for 5 seconds

        val fakeCallIntent = Intent(this, FakeCallActivity::class.java)
        startActivity(fakeCallIntent)
    }

    // --- End New Functionalities ---

    // Check for service when returning to the app
    override fun onResume() {
        super.onResume()
        updateSwitchState()
        loadPreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        // Release MediaPlayer resources if it's still playing
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
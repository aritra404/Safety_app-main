package aritra.seal.finalyearapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException
import java.util.Locale

class HelpDetectionService : Service() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var wakeLock: PowerManager.WakeLock? = null

    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private val RECORDING_DURATION_MS = 20 * 1000L // 20 seconds

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "HelpDetectionChannel"
    private val EMERGENCY_CHANNEL_ID = "emergency_channel"

    private val firebaseStorage = Firebase.storage
    private val storageRef = firebaseStorage.reference

    // Flag to prevent multiple emergency triggers
    private var emergencyInProgress = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupSpeechRecognizer()

        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HelpDetector::BackgroundServiceWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes

        // Create notification channels immediately
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startListening()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getCurrentDetectionWord(): String {
        val sharedPref = getSharedPreferences("HelpDetectorPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("detectionWord", "help") ?: "help"
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
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                android.util.Log.d("HelpDetectionService", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                android.util.Log.d("HelpDetectionService", "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                android.util.Log.d("HelpDetectionService", "End of speech, restarting...")
                // Restart listening when speech ends
                android.os.Handler(mainLooper).postDelayed({
                    if (!emergencyInProgress) {
                        startListening()
                    }
                }, 500)
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error: $error"
                }
                android.util.Log.e("HelpDetectionService", errorMessage)

                // Restart listening after a delay if not in emergency
                if (!emergencyInProgress) {
                    android.os.Handler(mainLooper).postDelayed({
                        startListening()
                    }, 2000)
                }
            }

            override fun onResults(results: android.os.Bundle?) {
                processRecognitionResults(results)
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                processRecognitionResults(partialResults)
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
    }

    private fun processRecognitionResults(results: android.os.Bundle?) {
        if (emergencyInProgress) return // Prevent multiple triggers

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val detectionWord = getCurrentDetectionWord()

        if (matches != null) {
            for (result in matches) {
                android.util.Log.d("HelpDetectionService", "Recognized: $result")
                if (result.lowercase().contains(detectionWord.lowercase())) {
                    emergencyInProgress = true
                    android.util.Log.i("HelpDetectionService", "${detectionWord.uppercase()} DETECTED!")

                    // Show emergency notification immediately
                    showEmergencyNotification(detectionWord)

                    // Get the guardian's number
                    val sharedPref = getSharedPreferences("HelpDetectorPrefs", Context.MODE_PRIVATE)
                    val guardianNumber = sharedPref.getString("guardianNumber", "")

                    if (!guardianNumber.isNullOrEmpty()) {
                        android.util.Log.i("HelpDetectionService", "Starting emergency sequence for: $guardianNumber")
                        handleEmergencySequence(guardianNumber)
                    } else {
                        android.util.Log.e("HelpDetectionService", "Guardian number not set!")
                        showToast("Guardian number not set!")
                        emergencyInProgress = false
                    }
                    break
                }
            }
        }

        // Restart listening if no emergency detected
        if (!emergencyInProgress) {
            android.os.Handler(mainLooper).postDelayed({
                startListening()
            }, 500)
        }
    }

    private fun handleEmergencySequence(guardianNumber: String) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val tempWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HelpDetector::EmergencyWakeLock"
        )
        tempWakeLock.acquire(2 * 60 * 1000L) // 2 minutes

        try {
            android.util.Log.i("HelpDetectionService", "Step 1: Sending initial SMS")
            sendEmergencySMS(guardianNumber)

            android.util.Log.i("HelpDetectionService", "Step 2: Starting audio recording")
            startAudioRecording()

            // Schedule the call and audio upload after recording
            android.os.Handler(mainLooper).postDelayed({
                android.util.Log.i("HelpDetectionService", "Step 3: Stopping recording and making call")

                // Stop recording first
                stopAudioRecordingAndUpload(guardianNumber) { success ->
                    android.util.Log.i("HelpDetectionService", "Audio upload completed: $success")
                }

                // Make call immediately (don't wait for upload)
                android.os.Handler(mainLooper).postDelayed({
                    android.util.Log.i("HelpDetectionService", "Step 4: Initiating emergency call")
                    makeEmergencyCall(guardianNumber)

                    // Reset emergency flag after call attempt
                    android.os.Handler(mainLooper).postDelayed({
                        emergencyInProgress = false
                        android.util.Log.i("HelpDetectionService", "Emergency sequence complete, resuming listening")
                        startListening()

                        if (tempWakeLock.isHeld) {
                            tempWakeLock.release()
                        }
                    }, 5000)
                }, 1000)

            }, RECORDING_DURATION_MS)

        } catch (e: Exception) {
            android.util.Log.e("HelpDetectionService", "Emergency sequence failed: ${e.message}")
            emergencyInProgress = false
            if (tempWakeLock.isHeld) {
                tempWakeLock.release()
            }
            startListening() // Resume listening on error
        }
    }

    private fun makeEmergencyCall(guardianNumber: String) {
        android.util.Log.i("HelpDetectionService", "Attempting to make emergency call to: $guardianNumber")

        try {
            // Check if CALL_PHONE permission is granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.e("HelpDetectionService", "CALL_PHONE permission not granted")
                showToast("Call permission not granted. Opening dialer...")

                // Fallback to dialer
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$guardianNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(dialIntent)
                showToast("Please tap call button to contact guardian")
                return
            }

            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$guardianNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivity(callIntent)
            android.util.Log.i("HelpDetectionService", "Emergency call initiated successfully")
            showToast("Calling guardian: $guardianNumber")

        } catch (e: Exception) {
            android.util.Log.e("HelpDetectionService", "Emergency call failed: ${e.message}")
            showToast("Call failed. Trying dialer...")

            // Fallback: Open dialer
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$guardianNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(dialIntent)
                showToast("Please tap call to contact guardian")
            } catch (dialException: Exception) {
                android.util.Log.e("HelpDetectionService", "Dialer fallback failed: ${dialException.message}")
                showToast("Unable to make call. Check permissions.")
            }
        }
    }

    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("HelpDetectionService", "RECORD_AUDIO permission not granted")
            showToast("Audio recording permission not granted")
            return
        }

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            audioFilePath = "${externalCacheDir?.absolutePath}/emergency_audio_${System.currentTimeMillis()}.mp3"

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
                android.util.Log.i("HelpDetectionService", "Audio recording started: $audioFilePath")
                showToast("Recording emergency audio...")
            }
        } catch (e: Exception) {
            android.util.Log.e("HelpDetectionService", "Failed to start audio recording: ${e.message}")
            showToast("Failed to start audio recording")
            mediaRecorder = null
            audioFilePath = null
        }
    }

    private fun stopAudioRecordingAndUpload(guardianNumber: String, onComplete: ((Boolean) -> Unit)? = null) {
        mediaRecorder?.apply {
            try {
                stop()
                release()
                android.util.Log.i("HelpDetectionService", "Audio recording stopped")

                audioFilePath?.let { path ->
                    val audioFile = File(path)
                    if (audioFile.exists() && audioFile.length() > 0) {
                        android.util.Log.i("HelpDetectionService", "Audio file exists, uploading...")
                        uploadAudioToFirebase(path, guardianNumber) { success ->
                            onComplete?.invoke(success)
                        }
                    } else {
                        android.util.Log.e("HelpDetectionService", "Audio file empty or missing")
                        onComplete?.invoke(false)
                    }
                } ?: run {
                    onComplete?.invoke(false)
                }
            } catch (e: RuntimeException) {
                android.util.Log.e("HelpDetectionService", "Failed to stop recording: ${e.message}")
                onComplete?.invoke(false)
            } finally {
                mediaRecorder = null
                audioFilePath = null
            }
        } ?: run {
            onComplete?.invoke(false)
        }
    }

    private fun uploadAudioToFirebase(filePath: String, guardianNumber: String, onComplete: ((Boolean) -> Unit)? = null) {
        val file = Uri.fromFile(File(filePath))
        val audioFileName = "emergency_audio/${System.currentTimeMillis()}_${file.lastPathSegment}"
        val audioRef = storageRef.child(audioFileName)

        android.util.Log.i("HelpDetectionService", "Starting Firebase upload: $audioFileName")
        showToast("Uploading emergency audio...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRef.putFile(file).await()
                val downloadUrl = audioRef.downloadUrl.await()
                android.util.Log.i("HelpDetectionService", "Audio uploaded successfully")

                // Send audio link via SMS
                sendAudioLinkSMS(guardianNumber, downloadUrl.toString())

                // Delete local file
                File(filePath).delete()
                onComplete?.invoke(true)

            } catch (e: Exception) {
                android.util.Log.e("HelpDetectionService", "Firebase upload failed: ${e.message}")
                showToast("Audio upload failed: ${e.message}")
                onComplete?.invoke(false)
            }
        }
    }

    private fun sendAudioLinkSMS(phoneNumber: String, audioUrl: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("HelpDetectionService", "SEND_SMS permission not granted")
            return
        }

        try {
            val message = "URGENT: Emergency audio recording: $audioUrl"
            val smsManager = SmsManager.getDefault()

            if (message.length > 160) {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                android.util.Log.i("HelpDetectionService", "Multi-part audio link SMS sent")
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                android.util.Log.i("HelpDetectionService", "Audio link SMS sent")
            }
            showToast("Audio link sent to guardian")

        } catch (e: Exception) {
            android.util.Log.e("HelpDetectionService", "Failed to send audio link SMS: ${e.message}")
            showToast("Failed to send audio link")
        }
    }

    private fun sendEmergencySMS(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("HelpDetectionService", "SEND_SMS permission not granted")
            showToast("SMS permission not granted")
            return
        }

        try {
            val message = "EMERGENCY: I need help! This is an urgent alert from my safety app."
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            android.util.Log.i("HelpDetectionService", "Emergency SMS sent successfully")
            showToast("Emergency SMS sent")

            // Send location in separate SMS
            getCurrentLocation { location ->
                if (location != null) {
                    val locationUrl = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                    val locationMessage = "My current location: $locationUrl"
                    try {
                        smsManager.sendTextMessage(phoneNumber, null, locationMessage, null, null)
                        android.util.Log.i("HelpDetectionService", "Location SMS sent")
                        showToast("Location sent to guardian")
                    } catch (e: Exception) {
                        android.util.Log.e("HelpDetectionService", "Location SMS failed: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HelpDetectionService", "Failed to send emergency SMS: ${e.message}")
            showToast("Failed to send SMS: ${e.message}")
        }
    }

    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.util.Log.e("HelpDetectionService", "Location permission not granted")
            callback(null)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                callback(location)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("HelpDetectionService", "Failed to get location: ${e.message}")
                callback(null)
            }
    }

    private fun showEmergencyNotification(detectionWord: String) {
        android.util.Log.i("HelpDetectionService", "Showing emergency notification")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, EmergencyHandlerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("GUARDIAN_NUMBER", getSharedPreferences("HelpDetectorPrefs", Context.MODE_PRIVATE).getString("guardianNumber", ""))
            putExtra("DETECTION_WORD", detectionWord)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 EMERGENCY ALERT 🚨")
            .setContentText("'${detectionWord.uppercase()}' detected! Contacting guardian...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            .build()

        try {
            notificationManager.notify(1002, notification)
            android.util.Log.i("HelpDetectionService", "Emergency notification shown")
        } catch (e: Exception) {
            android.util.Log.e("HelpDetectionService", "Failed to show notification: ${e.message}")
        }

        // Vibrate device
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500, 1000, 500, 1000), -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000), -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("HelpDetectionService", "Vibration failed: ${e.message}")
        }

        showToast("🚨 ${detectionWord.uppercase()} DETECTED! Alerting guardian...")
    }

    private fun showToast(message: String) {
        android.os.Handler(mainLooper).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun startListening() {
        if (::speechRecognizer.isInitialized && !emergencyInProgress) {
            try {
                speechRecognizer.startListening(recognizerIntent)
                android.util.Log.d("HelpDetectionService", "Started listening for speech")
            } catch (e: Exception) {
                android.util.Log.e("HelpDetectionService", "Failed to start listening: ${e.message}")
                android.os.Handler(mainLooper).postDelayed({
                    startListening()
                }, 2000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Regular service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Word Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running in background to detect help commands"
                setShowBadge(false)
            }

            // Emergency channel
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, null)
                setShowBadge(true)
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(emergencyChannel)
            android.util.Log.i("HelpDetectionService", "Notification channels created")
        }
    }

    private fun createNotification(): Notification {
        val detectionWord = getCurrentDetectionWord()
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity2::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎤 Word Detection Active")
            .setContentText("Listening for '$detectionWord' commands")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.i("HelpDetectionService", "Service being destroyed")

        emergencyInProgress = false

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        mediaRecorder?.release()
        mediaRecorder = null

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}
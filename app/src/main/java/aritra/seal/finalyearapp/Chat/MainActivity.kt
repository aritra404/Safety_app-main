package aritra.seal.finalyearapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings


class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private val appID = "27770458febb23d2"
    private val region = "IN"
    private val authKey = "266c684bf58feda9602ff9062128ba19cce9c865"

    private val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
        .setRegion(region)
        .setAppId(appID)
        .setAuthKey(authKey)
        .subscribePresenceForAllUsers()
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        CometChatUIKit.init(this, uiKitSettings, object : CometChat.CallbackListener<String?>() {
            override fun onSuccess(successString: String?) {
                Log.d(TAG, "Initialization completed successfully")
                checkUserLoginStatus()
            }

            override fun onError(e: CometChatException?) {
                Log.e(TAG, "Initialization failed: ${e?.message}")
                // Still show login screen even if init fails
                navigateToLogin()
            }
        })
    }

    private fun checkUserLoginStatus() {
        val loggedInUser = CometChat.getLoggedInUser()

        if (loggedInUser != null) {
            Log.d(TAG, "User already logged in: ${loggedInUser.uid}")
            // User is already logged in, go to main app
            navigateToTabbedActivity()
        } else {
            Log.d(TAG, "No user logged in, showing login screen")
            // No user logged in, show login screen
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity so user can't go back
    }

    private fun navigateToTabbedActivity() {
        val intent = Intent(this, TabbedActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity so user can't go back
    }
}



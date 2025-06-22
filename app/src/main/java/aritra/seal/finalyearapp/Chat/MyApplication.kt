package aritra.seal.finalyearapp


import android.app.Application
import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings

class MyApplication : Application() {


    private val TAG = "MyApplication"

    private val appID = "27770458febb23d2"
    private val region = "IN"
    private val authKey = "266c684bf58feda9602ff9062128ba19cce9c865"

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Application onCreate - Initializing CometChat")

        val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
            .setRegion(region)
            .setAppId(appID)
            .setAuthKey(authKey)
            .subscribePresenceForAllUsers()
            .build()

        CometChatUIKit.init(this, uiKitSettings, object : CometChat.CallbackListener<String?>() {
            override fun onSuccess(successString: String?) {
                Log.d(TAG, "CometChat initialization completed successfully")
            }

            override fun onError(e: CometChatException?) {
                Log.e(TAG, "CometChat initialization failed: ${e?.message}")
            }
        })
    }
}
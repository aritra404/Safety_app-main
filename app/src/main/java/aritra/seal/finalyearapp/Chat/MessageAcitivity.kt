package aritra.seal.finalyearapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.messagecomposer.CometChatMessageComposer
import com.cometchat.chatuikit.messageheader.CometChatMessageHeader
import com.cometchat.chatuikit.messagelist.CometChatMessageList


class MessageActivity : AppCompatActivity() {
    private lateinit var messageHeader: CometChatMessageHeader
    private lateinit var messageList: CometChatMessageList
    private lateinit var messageComposer: CometChatMessageComposer

    private var uid: String? = null
    private var guid: String? = null
    private var receiverName: String? = null

    private val TAG = "MessageActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message)

        setupWindowInsets()
        initializeViews()
        setupChat()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        messageHeader = findViewById(R.id.message_header)
        messageList = findViewById(R.id.message_list)
        messageComposer = findViewById(R.id.message_composer)
    }

    private fun setupChat() {
        uid = intent.getStringExtra("uid")
        guid = intent.getStringExtra("guid")
        receiverName = intent.getStringExtra("name")

        when {
            !uid.isNullOrEmpty() -> setupUserChat(uid!!)
            !guid.isNullOrEmpty() -> setupGroupChat(guid!!)
            else -> {
                Log.e(TAG, "No user ID or group ID provided")
                showError("Missing user ID or group ID")
                finish()
            }
        }
    }

    private fun setupUserChat(userId: String) {
        Log.d(TAG, "Setting up user chat for: $userId")

        CometChat.getUser(userId, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                runOnUiThread {
                    Log.d(TAG, "Successfully loaded user: ${user.uid}")
                    try {
                        // Set user for all components
                        messageHeader.setUser(user)
                        messageList.setUser(user)
                        messageComposer.setUser(user)


                        Log.d(TAG, "All components configured successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error configuring UI components: ${e.message}")
                        showError("Error setting up chat interface")
                    }
                }
            }

            override fun onError(e: CometChatException?) {
                runOnUiThread {
                    Log.e(TAG, "Error loading user: ${e?.message}")
                    showError("Could not find user: ${e?.message}")
                    finish()
                }
            }
        })
    }

    private fun setupGroupChat(groupId: String) {
        Log.d(TAG, "Setting up group chat for: $groupId")

        CometChat.getGroup(groupId, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(group: Group) {
                runOnUiThread {
                    Log.d(TAG, "Successfully loaded group: ${group.guid}")
                    try {
                        // Set group for all components
                        messageHeader.setGroup(group)
                        messageList.setGroup(group)
                        messageComposer.setGroup(group)

                        Log.d(TAG, "All group components configured successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error configuring group UI components: ${e.message}")
                        showError("Error setting up group chat interface")
                    }
                }
            }

            override fun onError(e: CometChatException?) {
                runOnUiThread {
                    Log.e(TAG, "Error loading group: ${e?.message}")
                    showError("Could not find group: ${e?.message}")
                    finish()
                }
            }
        })
    }


    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Activity destroyed")
    }
}
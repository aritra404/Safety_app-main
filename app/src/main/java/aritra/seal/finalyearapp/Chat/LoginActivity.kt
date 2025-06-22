package aritra.seal.finalyearapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit


class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: TextView
    private lateinit var btnDemoLogin: Button

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        setupWindowInsets()
        initViews()
        setupClickListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignup = findViewById(R.id.tvSignup)
        btnDemoLogin = findViewById(R.id.btnDemoLogin)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isNotEmpty()) {
                loginUser(username)
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        btnDemoLogin.setOnClickListener {
            // Quick demo login with predefined users
            loginUser("cometchat-uid-1")
        }
    }

    private fun loginUser(uid: String) {
        // Show loading state
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "Login successful: ${user.uid}")
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Welcome ${user.name}!", Toast.LENGTH_SHORT).show()

                    // Navigate to main app
                    val intent = Intent(this@LoginActivity, TabbedActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "Login failed: ${e.message}")
                runOnUiThread {
                    // Reset button state
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"

                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        })
    }
}
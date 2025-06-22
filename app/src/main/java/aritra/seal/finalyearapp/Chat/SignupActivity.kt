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


class SignupActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etName: EditText
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView

    private val TAG = "SignupActivity"
    private val authKey = "266c684bf58feda9602ff9062128ba19cce9c865"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

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
        etName = findViewById(R.id.etName)
        btnSignup = findViewById(R.id.btnSignup)
        tvLogin = findViewById(R.id.tvLogin)
    }

    private fun setupClickListeners() {
        btnSignup.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val name = etName.text.toString().trim()

            if (validateInputs(username, name)) {
                createUser(username, name)
            }
        }

        tvLogin.setOnClickListener {
            finish() // Go back to login screen
        }
    }

    private fun validateInputs(username: String, name: String): Boolean {
        when {
            username.isEmpty() -> {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return false
            }
            name.isEmpty() -> {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return false
            }
            username.length < 3 -> {
                Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun createUser(uid: String, name: String) {
        // Show loading state
        btnSignup.isEnabled = false
        btnSignup.text = "Creating account..."

        val user = User().apply {
            this.uid = uid
            this.name = name
        }

        CometChat.createUser(user, authKey, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(createdUser: User) {
                Log.d(TAG, "User created successfully: ${createdUser.uid}")

                // After creating user, automatically log them in
                loginNewUser(createdUser.uid)
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "User creation failed: ${e.message}")
                runOnUiThread {
                    // Reset button state
                    btnSignup.isEnabled = true
                    btnSignup.text = "Sign Up"

                    val errorMessage = when {
                        e.message?.contains("already exists") == true -> "Username already exists. Please choose a different one."
                        else -> "Signup failed: ${e.message}"
                    }
                    Toast.makeText(this@SignupActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun loginNewUser(uid: String) {
        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Log.d(TAG, "Auto-login successful: ${user.uid}")
                runOnUiThread {
                    Toast.makeText(this@SignupActivity, "Account created! Welcome ${user.name}!", Toast.LENGTH_SHORT).show()

                    // Navigate to main app
                    val intent = Intent(this@SignupActivity, TabbedActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "Auto-login failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@SignupActivity, "Account created but login failed. Please try logging in manually.", Toast.LENGTH_LONG).show()

                    // Go back to login screen
                    finish()
                }
            }
        })
    }
}
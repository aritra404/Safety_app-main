package aritra.seal.finalyearapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment


import com.google.android.material.bottomnavigation.BottomNavigationView

class TabbedActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tabbed)

        setupWindowInsets()
        initViews()
        setupNavigation(savedInstanceState)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = createFragmentForNavItem(item.itemId)
            replaceFragment(fragment)
            true
        }

        // Set default fragment only when activity is first created
        if (savedInstanceState == null) {
            replaceFragment(ChatsFragment())
            bottomNavigationView.selectedItemId = R.id.nav_chats
        }
    }

    private fun createFragmentForNavItem(itemId: Int): Fragment {
        return when (itemId) {
            R.id.nav_chats -> ChatsFragment()
            R.id.nav_call_logs -> CallLogsFragment()
            R.id.nav_users -> UsersFragment()
            R.id.nav_groups -> GroupsFragment()
            else -> ChatsFragment()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
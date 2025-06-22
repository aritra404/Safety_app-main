package aritra.seal.finalyearapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.users.CometChatUsers

class UsersFragment : Fragment() {

    private lateinit var usersView: CometChatUsers

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_users, container, false)

        initView(view)
        setListeners()

        return view
    }

    private fun initView(view: View) {
        usersView = view.findViewById(R.id.users)
    }

    private fun setListeners() {
        usersView.setOnItemClick { _, _, user ->
            startMessageActivity(user)
        }
    }

    private fun startMessageActivity(user: User) {
        val intent = Intent(context, MessageActivity::class.java).apply {
            putExtra("uid", user.uid)
            putExtra("name", user.name)
        }
        startActivity(intent)
    }
}
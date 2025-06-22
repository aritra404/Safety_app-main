package aritra.seal.finalyearapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.conversations.CometChatConversations


class ChatsFragment : Fragment() {

    private lateinit var conversationsView: CometChatConversations

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_chats_fragment, container, false)

        initView(view)
        setListeners()

        return view
    }

    private fun initView(view: View) {
        conversationsView = view.findViewById(R.id.cometChatConversations)
    }

    private fun setListeners() {
        conversationsView.setOnItemClick { _, _, conversation ->
            startMessageActivity(conversation)
        }
    }

    private fun startMessageActivity(conversation: Conversation) {
        val intent = Intent(context, MessageActivity::class.java)

        when (conversation.conversationType) {
            "user" -> {
                val user = conversation.conversationWith as User
                intent.putExtra("uid", user.uid)
                intent.putExtra("name", user.name)
            }
            "group" -> {
                val group = conversation.conversationWith as Group
                intent.putExtra("guid", group.guid)
                intent.putExtra("name", group.name)
            }
        }

        startActivity(intent)
    }
}
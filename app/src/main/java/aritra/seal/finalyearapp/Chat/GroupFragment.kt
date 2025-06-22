package aritra.seal.finalyearapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chat.models.Group
import com.cometchat.chatuikit.groups.CometChatGroups


class GroupsFragment : Fragment() {

    private lateinit var groupsView: CometChatGroups

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)

        initView(view)
        setListeners()

        return view
    }

    private fun initView(view: View) {
        groupsView = view.findViewById(R.id.groups)
    }

    private fun setListeners() {
        groupsView.setOnItemClick { _, _, group ->
            startMessageActivity(group)
        }
    }

    private fun startMessageActivity(group: Group) {
        val intent = Intent(context, MessageActivity::class.java).apply {
            putExtra("guid", group.guid)
            putExtra("name", group.name)
        }
        startActivity(intent)
    }
}
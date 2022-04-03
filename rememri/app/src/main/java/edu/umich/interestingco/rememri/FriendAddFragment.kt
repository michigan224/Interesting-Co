package edu.umich.interestingco.rememri

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FriendAddFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendAddFragment : Fragment() {
    // TODO: Rename and change types of parameters

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val sharedPref : SharedPreferences?= activity?.getPreferences(Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")

        val view: View = inflater.inflate(R.layout.fragment_friend_add, container, false)
        val submitBtn = view.findViewById<Button>(R.id.submitButton)
        val usernameSearch = view.findViewById<EditText>(R.id.editTextSearch)

        submitBtn.setOnClickListener {
            val submitSearch = usernameSearch.text
            var apiRequest : String =
                "https://rememri-instance-5obwaiol5q-ue.a.run.app/friend_request?user1=$username&user2=$submitSearch&action=request"
        }

        return inflater.inflate(R.layout.fragment_friend_add, container, false)
    }
}
package edu.umich.interestingco.rememri

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import edu.umich.interestingco.rememri.FriendStore.friends
import edu.umich.interestingco.rememri.databinding.FragmentFriendListBinding


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FriendListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendListFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var friendListAdapter: FriendListAdapter
    private lateinit var view1: FragmentFriendListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {

        val sharedPref : SharedPreferences?= activity?.getPreferences(Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")

        val view: View = inflater.inflate(R.layout.fragment_friend_list, container, false)
        val myList: ListView = view.findViewById(R.id.friendList)
        friendListAdapter = FriendListAdapter(requireContext().applicationContext, friends)
        myList.adapter = friendListAdapter
        //view1.friendList.adapter = friendListAdapter
        
        // Inflate the layout for this fragment
        return view
    }
}
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
import androidx.databinding.ObservableList
import androidx.databinding.ObservableArrayList
import androidx.databinding.DataBindingUtil.setContentView
import edu.umich.interestingco.rememri.FriendStore.getFriends
import edu.umich.interestingco.rememri.databinding.FragmentFriendAddBinding
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
    private lateinit var myView : FragmentFriendListBinding
    var _binding: FragmentFriendListBinding? = null
    private val binding get() = _binding!!
//    var _binding: FragmentFriendListBinding? = null
//    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        _binding = FragmentFriendListBinding.inflate(inflater, container, false)
//        _binding = FragmentFriendListBinding.inflate(inflater, container, false)

        val sharedPref : SharedPreferences?= activity?.getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")

        friends.addOnListChangedCallback(propertyObserver)
//        myView = FragmentFriendListBinding.inflate(layoutInflater)

        friendListAdapter = FriendListAdapter(binding.root.context, friends)
        binding.friendList.adapter = friendListAdapter
//        myView.friendList.adapter = friendListAdapter

        binding.refreshContainer.setOnRefreshListener {
            refreshFriends()
        }

//        val view: View = inflater.inflate(R.layout.fragment_friend_list, container, false)
//        val myList: ListView = view.findViewById(R.id.friendList)
//        friendListAdapter = FriendListAdapter(requireContext().applicationContext, friends)
//        myList.adapter = friendListAdapter
        //view1.friendList.adapter = friendListAdapter
        
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun refreshFriends() {
        getFriends(activity)


        binding.refreshContainer.isRefreshing = false
    }

    private val propertyObserver = object: ObservableList.OnListChangedCallback<ObservableArrayList<Int>>() {
        override fun onChanged(sender: ObservableArrayList<Int>?) {}
        override fun onItemRangeChanged(
            sender: ObservableArrayList<Int>?,
            positionStart: Int,
            itemCount: Int
        ) {}

        override fun onItemRangeInserted(
            sender: ObservableArrayList<Int>?,
            positionStart: Int,
            itemCount: Int
        ) {
            println("onItemRangeInserted: $positionStart, $itemCount")
            activity?.runOnUiThread {
                friendListAdapter.notifyDataSetChanged()
            }
        }

        override fun onItemRangeMoved(
            sender: ObservableArrayList<Int>?,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) {}

        override fun onItemRangeRemoved(
            sender: ObservableArrayList<Int>?,
            positionStart: Int,
            itemCount: Int
        ) {}
    }

    override fun onDestroy() {
        super.onDestroy()

        friends.removeOnListChangedCallback(propertyObserver)
    }
}
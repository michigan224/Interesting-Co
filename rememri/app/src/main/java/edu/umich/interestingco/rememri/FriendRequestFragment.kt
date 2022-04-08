package edu.umich.interestingco.rememri

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.fragment.app.ListFragment
import edu.umich.interestingco.rememri.databinding.FragmentFriendRequestBinding
import edu.umich.interestingco.rememri.FriendStore.requests
import edu.umich.interestingco.rememri.FriendStore.getRequests

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FriendRequestFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendRequestFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var friendListAdapter: FriendRequestsAdapter
    var _binding: FragmentFriendRequestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        getRequests(activity)
        _binding = FragmentFriendRequestBinding.inflate(inflater, container, false)

        val sharedPref : SharedPreferences?= activity?.getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")

        requests.addOnListChangedCallback(propertyObserver)

        friendListAdapter = FriendRequestsAdapter(binding.root.context, requests)
        binding.requestList.adapter = friendListAdapter

        binding.refreshContainer.setOnRefreshListener {
            refreshFriends()
        }

        return binding.root
    }

    private fun refreshFriends() {
        getRequests(activity)


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

        requests.removeOnListChangedCallback(propertyObserver)
    }

}
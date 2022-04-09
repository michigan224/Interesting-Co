package edu.umich.interestingco.rememri

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.fragment.app.ListFragment
import coil.load
import com.squareup.picasso.Picasso
import edu.umich.interestingco.rememri.databinding.PostViewBinding
import edu.umich.interestingco.rememri.CommentStore.comments
import edu.umich.interestingco.rememri.CommentStore.getComments
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class PostActivity : AppCompatActivity() {
    private lateinit var friendListAdapter: CommentAdapter
    private var _binding: PostViewBinding? = null
    private val binding get() = _binding!!
    private val client = OkHttpClient()
    var myPostId : Int? = null
    var myImage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var myIntent = intent.extras
        myPostId = myIntent?.getInt("pin_id")
        getComments(this, myPostId)
        val mimageView = binding.imageCard
        val sharedPref : SharedPreferences?= getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")
        val token = sharedPref?.getString("token", "")
        val userUrl = "https://rememri-instance-5obwaiol5q-ue.a.run.app/pin/$myPostId?username=$username"

        val request = okhttp3.Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("getFriends", "Failed GET request")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val myJSON = try {
                        JSONObject(response.body?.string() ?: "").getJSONArray("media_url")
                    } catch (e: JSONException) {
                        JSONArray()
                    }
                    myImage = myJSON.toString()
                }
            }
        })

        Picasso.get().load(myImage).into(mimageView)

        _binding = PostViewBinding.inflate(layoutInflater)

        comments.addOnListChangedCallback(propertyObserver)

        friendListAdapter = CommentAdapter(binding.root.context, comments)
        binding.commentList.adapter = friendListAdapter

        binding.refreshContainer.setOnRefreshListener {
            refreshFriends()
        }

        if (token == "") {
            binding.commentButton.visibility = View.GONE
        }

        binding.commentButton.setOnClickListener {
            binding.commentSubmitButton.visibility = View.VISIBLE
            binding.commentBox.visibility = View.VISIBLE
            binding.commentButton.visibility = View.GONE
        }

        val commentText = binding.commentBox
        val commentSubmit = binding.commentSubmitButton

        commentSubmit.setOnClickListener {
            val submitText = commentText.text
        }

    }

    private fun refreshFriends() {
        getComments(this, myPostId)


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
            runOnUiThread {
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

        comments.removeOnListChangedCallback(propertyObserver)
    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))
}
package edu.umich.interestingco.rememri

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.os.Bundle
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import com.squareup.picasso.Picasso
import edu.umich.interestingco.rememri.CommentStore.comments
import edu.umich.interestingco.rememri.CommentStore.getComments
import edu.umich.interestingco.rememri.databinding.ActivityPinViewBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch


class PinViewActivity : AppCompatActivity() {
    private lateinit var friendListAdapter: CommentAdapter
    //private var _binding: ActivityPinViewBinding? = null
    // private val binding get() = _binding!!
    //private val binding = ActivityPinViewBinding.inflate(layoutInflater)
    private lateinit var binding: ActivityPinViewBinding
    private val client = OkHttpClient()
    var myPostId : String? = null
    var myImage = ""

    private lateinit var url: URL
    private lateinit var urlConnection: HttpURLConnection
    private lateinit var commentObj: JSONObject

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var myIntent = intent.extras
        myPostId = myIntent?.getString("pin_id")
        getComments(this, myPostId)
        binding = ActivityPinViewBinding.inflate(layoutInflater)
        val mimageView = binding.imageCard
        val sharedPref : SharedPreferences?= getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")
        val token = sharedPref?.getString("token", "")
        val userUrl = "https://rememri-instance-5obwaiol5q-ue.a.run.app/pin/$myPostId?username=$username"

        val request = okhttp3.Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val countDownLatch = CountDownLatch(1)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("getFriends", "Failed GET request")
                countDownLatch.countDown();
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val myJSON = try {
                        JSONObject(response.body?.string() ?: "").get("media_url")
                    } catch (e: JSONException) {
                        JSONArray()
                    }
                    myImage = myJSON.toString()
                    countDownLatch.countDown();
                }
            }
        })
        countDownLatch.await()
        val imageAsBytes: ByteArray = Base64.decode(myImage, Base64.DEFAULT)
        val bitmapImage = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
        val stream = ByteArrayOutputStream()
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        mimageView.setImageBitmap(bitmapImage)
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

        // TODO : need to test this once 2D pin viewing is possible
        commentSubmit.setOnClickListener {
            val submitText = commentText.text

            val oldSharedPref = getSharedPreferences("mypref", 0)
            val token = oldSharedPref.getString("token", "")

            // TODO : not sure if we need to do any redirection for not logged in users...
            if (token == "") {
                Log.d("COMMENT PERMISSION", "User not logged in, cannot post comment")
            } else {
                url = URL("https://rememri-instance-5obwaiol5q-ue.a.run.app/comment")
                commentObj = JSONObject()
                commentObj.put("comment_text", submitText)

                val commentString = commentObj.toString()

                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.doOutput = true
                urlConnection.doInput = true

                val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
                StrictMode.setThreadPolicy(policy)
                val outputSt = OutputStreamWriter(urlConnection.outputStream)
                outputSt.write(commentString)
                outputSt.flush()

                val response = urlConnection.responseCode
                if (response == HttpURLConnection.HTTP_OK) {
                    Log.d("COMMENT", "HTTP request for comment submission went through")
                } else {
                    Log.e("HTTPURLCONNECTION_ERROR", response.toString())
                }

            }
        }
        setContentView(binding.root)
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
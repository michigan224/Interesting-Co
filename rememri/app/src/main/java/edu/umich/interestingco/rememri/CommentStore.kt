package edu.umich.interestingco.rememri

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.reflect.full.declaredMemberProperties
import android.content.SharedPreferences
import androidx.databinding.ObservableArrayList
import okhttp3.*
import java.io.IOException
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import kotlin.reflect.full.declaredMemberProperties
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody

object CommentStore {
    val comments = arrayListOf<Comment?>()
    private val nFields = Comment::class.declaredMemberProperties.size

    private lateinit var queue: RequestQueue

    private const val serverUrl = "https://rememri-instance-5obwaiol5q-ue.a.run.app"

    private val client = OkHttpClient()
    fun postComment(context: Context, comment: Comment) {
        val jsonObj = mapOf(
            "username" to comment.owner_id,
            "comment_text" to comment.text,
            "post_id" to comment.post_id,
        )
        val postRequest = JsonObjectRequest(
            Request.Method.POST,
            serverUrl+"comment/", JSONObject(jsonObj),
            { Log.d("comment", "comment posted!") },
            { error -> Log.e("comment", error.localizedMessage ?: "JsonObjectRequest error") }
        )

        if (!this::queue.isInitialized) {
            queue = newRequestQueue(context)
        }
        queue.add(postRequest)
    }

    fun getComments(activity: PostActivity?, pinId: Int) {
        val oldSharedPref = activity?.getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = oldSharedPref?.getString("username", "")
        val token = oldSharedPref?.getString("token", "")

        val userUrl = serverUrl + "pin/$pinId?username=$username"
        val request = Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("getFriends", "Failed GET request")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // val friendsReceived = try { JSONObject(response.body?.string() ?: "").getJSONArray("") } catch (e: JSONException) { JSONArray() }
                    val resp: String? = response.body?.string()?.replace("\n", "")
                    val commentsReceived: Array<String>? = if (resp == "[]"){
                        null
                    } else {
                        resp?.replace("[", "")?.replace("]", "")
                            ?.replace("\"", "")?.split(",")
                            ?.toTypedArray()

                    }
                    comments.clear()
                    if (commentsReceived != null) {
                        for (comment in commentsReceived) {
                            comments.add(Comment(post_id = pinId))
                        }
                    }
                }
            }
        })
    }
}
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL
import kotlin.reflect.full.declaredMemberProperties

object CommentStore {
    val comments = ObservableArrayList<Comment?>()
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

    val myCommentsReturn = ObservableArrayList<Comment?>()
    private lateinit var urlConnection: HttpURLConnection

    fun getComments(context: Context, pinId: Int?) {
        val oldSharedPref = context.getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = oldSharedPref?.getString("username", "")
        val token = oldSharedPref?.getString("token", "")

        val userUrl = URL(serverUrl + "pin/$pinId?username=$username&comments_only=true")
        urlConnection = userUrl.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("Authorization", "Bearer $token")

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
                    val data = response.body?.string()
                    val gson = Gson()
                    val myComments = object : TypeToken<List<Comment>>() {}.type
                    val comments = gson.fromJson<List<Comment>>(data, myComments)
//                    val comments = try { JSONObject(response.body?.string() ?: "").getJSONArray("comments") } catch (e: JSONException) { JSONArray() }
                    // val resp: String? = response.body?.string()?.replace("\n", "")

//                    myComments.clear()
                    for (element in comments) {
                        val commentEntry = element
                            myCommentsReturn.add(Comment(comment_id = element.comment_id,
                                text = element.text,
                                timestamp = element.timestamp,
                                post_id = element.post_id,
                                owner_id = element.owner_id,
                                is_owned_by_user = element.is_owned_by_user,
                            ))
                    }
                }
            }
        })
    }
}

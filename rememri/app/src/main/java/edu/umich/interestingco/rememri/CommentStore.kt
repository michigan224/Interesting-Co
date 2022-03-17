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

object CommentStore {
    val comments = arrayListOf<Comment?>()
    private val nFields = Comment::class.declaredMemberProperties.size

    private lateinit var queue: RequestQueue
    private const val serverUrl = "https://temp/"
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
}
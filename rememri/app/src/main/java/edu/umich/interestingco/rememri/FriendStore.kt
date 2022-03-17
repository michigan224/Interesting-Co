package edu.umich.interestingco.rememri

import android.content.Context
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableArrayList
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.reflect.full.declaredMemberProperties

object FriendStore {
    private val nFields = Friend::class.declaredMemberProperties.size

    private const val serverUrl = "https://18.222.21.16/"

    private val client = OkHttpClient()

    val friends = ObservableArrayList<Friend?>()

    fun getFriends() {
        val request = Request.Builder()
            .url(serverUrl+"getfriends/")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("getFriends", "Failed GET request")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val friendsReceived = try { JSONObject(response.body?.string() ?: "").getJSONArray("chatts") } catch (e: JSONException) { JSONArray() }

                    friends.clear()
                    for (i in 0 until friendsReceived.length()) {
                        val chattEntry = friendsReceived[i] as JSONArray
                        if (chattEntry.length() == nFields) {
                            friends.add(Friend(username = chattEntry[0].toString()
                            ))
                        } else {
                            Log.e("getFriends", "Received unexpected number of fields " + chattEntry.length().toString() + " instead of " + nFields.toString())
                        }
                    }
                }
            }
        })
    }
}
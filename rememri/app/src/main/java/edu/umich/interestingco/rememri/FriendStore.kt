package edu.umich.interestingco.rememri

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.databinding.ObservableArrayList
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import android.content.Intent
import kotlin.reflect.full.declaredMemberProperties

object FriendStore {
    private val nFields = Friend::class.declaredMemberProperties.size

    private const val serverUrl = "https://rememri-instance-5obwaiol5q-ue.a.run.app/"

    private val client = OkHttpClient()

    val friends = ObservableArrayList<Friend?>()

    fun getFriends(activity: FriendActivity) {
        val oldSharedPref = activity.getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = oldSharedPref?.getString("username", "")
        val token = oldSharedPref?.getString("token", "")

        val userUrl = serverUrl + "friends?username=" + username
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
                    val friendsReceived: Array<String>? = response.body?.string()?.replace("[", "")?.replace("]", "")?.replace("\"", "")?.replace("\n","")?.split(",")?.toTypedArray()
                    friends.clear()
                    if (friendsReceived != null) {
                        for (username in friendsReceived) {
                            friends.add(Friend(username = username))
                        }
                    }
//                    for (i in 0 until friendsReceived.size) {
//                        val chattEntry = friendsReceived[i] as JSONArray
//                        if (chattEntry.length() == nFields) {
//                            friends.add(Friend(username = chattEntry[0].toString()
//                            ))
//                        } else {
//                            Log.e("getFriends", "Received unexpected number of fields " + chattEntry.length().toString() + " instead of " + nFields.toString())
//                        }
//                    }
                }
            }
        })
    }
}
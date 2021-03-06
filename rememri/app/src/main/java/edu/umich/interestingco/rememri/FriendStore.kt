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
import androidx.fragment.app.FragmentActivity
import kotlin.reflect.full.declaredMemberProperties

object FriendStore {
    private val nFields = Friend::class.declaredMemberProperties.size

    private const val serverUrl = "https://rememri-instance-5obwaiol5q-ue.a.run.app/"

    private val client = OkHttpClient()

    val friends = ObservableArrayList<Friend?>()
    val requests = ObservableArrayList<Friend?>()

    fun getFriends(activity: FragmentActivity?) {
        val oldSharedPref = activity?.getSharedPreferences("mypref", Context.MODE_PRIVATE)
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
                        val resp: String? = response.body?.string()?.replace("\n", "")
                    val friendsReceived: Array<String>? = if (resp == "[]"){
                        null
                    } else {
                        resp?.replace("[", "")?.replace("]", "")
                            ?.replace("\"", "")?.split(",")
                            ?.toTypedArray()

                    }
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

    fun getRequests(activity: FragmentActivity?) {
        val oldSharedPref = activity?.getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = oldSharedPref?.getString("username", "")
        val token = oldSharedPref?.getString("token", "")

        val userUrl = "$serverUrl/friend_requests/incoming?username=$username"
        val request = Request.Builder()
            .url(userUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("getFriendRequestsIncoming", "Failed GET request")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val resp: String? = response.body?.string()?.replace("\n", "")
                    val requestsReceived: Array<String>? = if (resp == "[]"){
                        null
                    } else {
                        resp?.replace("[", "")?.replace("]", "")
                            ?.replace("\"", "")?.split(",")
                            ?.toTypedArray()

                    }
                    requests.clear()
                    if (requestsReceived != null) {
                        for (username in requestsReceived) {
                            requests.add(Friend(username = username, relationship = "incoming"))
                        }
                    }
                }
            }
        })
    }
}
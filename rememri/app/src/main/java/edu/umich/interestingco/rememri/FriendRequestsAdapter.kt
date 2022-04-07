package edu.umich.interestingco.rememri

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import edu.umich.interestingco.rememri.databinding.ListitemRequestsBinding
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FriendRequestsAdapter(context: Context, users: ArrayList<Friend?>) :
    ArrayAdapter<Friend?>(context, 0, users) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val sharedPref : SharedPreferences?= context.getSharedPreferences("mypref", Context.MODE_PRIVATE)
            val user = sharedPref?.getString("username", "")
            val token = sharedPref?.getString("token", "")
            val listItemView = (convertView?.tag /* reuse binding */ ?: run {
                val rowView =
                    LayoutInflater.from(context).inflate(R.layout.listitem_requests, parent, false)
                rowView.tag = ListitemRequestsBinding.bind(rowView) // cache binding
                rowView.tag
            }) as ListitemRequestsBinding

            getItem(position)?.run {
                listItemView.usernameTextView.text = username
                listItemView.buttonAccept.setOnClickListener {
                    var apiRequest =
                        "https://rememri-instance-5obwaiol5q-ue.a.run.app/friend_request"
                    var url = URL(apiRequest)

                    var request = JSONObject()
                    request.put("user1", user)
                    request.put("user2", username)
                    request.put("action", "accept")

                    val requestString = request.toString()

                    var urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Accept", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $token")
                    urlConnection.doOutput = true
                    urlConnection.doInput = true

                    val policy = StrictMode.ThreadPolicy.Builder()
                        .permitAll().build()
                    StrictMode.setThreadPolicy(policy)
                    val outputSt = OutputStreamWriter(urlConnection.outputStream)
                    outputSt.write(requestString)
                    outputSt.flush()

                    val response = urlConnection.responseCode
                    val myResp = urlConnection.responseMessage
                    if (response == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(context, "Friend successfully added", Toast.LENGTH_LONG)
                            .show()
                    } else if (response == HttpURLConnection.HTTP_BAD_REQUEST) {
                        Toast.makeText(context, "You are already friends!", Toast.LENGTH_LONG)
                            .show()
                    } else if (response == HttpURLConnection.HTTP_CONFLICT) {
                        Toast.makeText(context, "requested user doesn't exist", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Log.e("HTTPURLCONNECTION_ERROR", response.toString())
                        Toast.makeText(context, "Connection Error", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                listItemView.buttonReject.setOnClickListener {
                    var apiRequest =
                        "https://rememri-instance-5obwaiol5q-ue.a.run.app/friend_request"
                    var url = URL(apiRequest)

                    var request = JSONObject()
                    request.put("user1", user)
                    request.put("user2", username)
                    request.put("action", "reject")

                    val requestString = request.toString()

                    var urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "POST"
                    urlConnection.setRequestProperty("Content-Type", "application/json")
                    urlConnection.setRequestProperty("Accept", "application/json")
                    urlConnection.setRequestProperty("Authorization", "Bearer $token")
                    urlConnection.doOutput = true
                    urlConnection.doInput = true

                    val policy = StrictMode.ThreadPolicy.Builder()
                        .permitAll().build()
                    StrictMode.setThreadPolicy(policy)
                    val outputSt = OutputStreamWriter(urlConnection.outputStream)
                    outputSt.write(requestString)
                    outputSt.flush()

                    val response = urlConnection.responseCode
                    val myResp = urlConnection.responseMessage
                    if (response == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(context, "Friend successfully added", Toast.LENGTH_LONG)
                            .show()
                    } else if (response == HttpURLConnection.HTTP_BAD_REQUEST) {
                        Toast.makeText(context, "Friend request not found", Toast.LENGTH_LONG)
                            .show()
                    } else if (response == HttpURLConnection.HTTP_CONFLICT) {
                        Toast.makeText(context, "requested user doesn't exist", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Log.e("HTTPURLCONNECTION_ERROR", response.toString())
                        Toast.makeText(context, "Connection Error", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                //listItemView.root.setBackgroundColor(Color.parseColor(if (position % 2 == 0) "#E0E0E0" else "#EEEEEE"))
            }

            return listItemView.root

        }
    }
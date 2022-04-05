package edu.umich.interestingco.rememri

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.google.gson.Gson
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private lateinit var request: JSONObject

/**
 * A simple [Fragment] subclass.
 * Use the [FriendAddFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FriendAddFragment : Fragment() {
    // TODO: Rename and change types of parameters

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val sharedPref : SharedPreferences?= activity?.getPreferences(Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")
        val token = sharedPref?.getString("token", "")

        val view: View = inflater.inflate(R.layout.fragment_friend_add, container, false)

        val submitBtn = view.findViewById<Button>(R.id.submitRequestButton)
        val usernameSearch = view.findViewById<EditText>(R.id.editTextSearch)

        submitBtn.setOnClickListener { view ->
            val submitSearch = usernameSearch.text
            var apiRequest : String =
                "https://rememri-instance-5obwaiol5q-ue.a.run.app/friend_request"
            var url : URL = URL(apiRequest)
            request.put("user1", username)
            request.put("user2", submitSearch.toString())
            request.put("action", "request")

            val requestString = request.toString()

            Toast.makeText(activity, "JSON created", Toast.LENGTH_LONG)
                .show()

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
                Toast.makeText(activity, "Friend successfully added", Toast.LENGTH_LONG)
                    .show()
            } else if (response == HttpURLConnection.HTTP_BAD_REQUEST) {
                Toast.makeText(activity, myResp.toString(), Toast.LENGTH_LONG)
                    .show()
            } else if (response == HttpURLConnection.HTTP_CONFLICT) {
                Toast.makeText(activity, "requested user doesn't exist", Toast.LENGTH_LONG)
                    .show()
            } else {
                Log.e("HTTPURLCONNECTION_ERROR", response.toString())
                Toast.makeText(activity, "Connection Error", Toast.LENGTH_LONG)
                    .show()
            }
        }

        return view
    }

//    fun submitRequest() {
//        val sharedPref : SharedPreferences?= activity?.getPreferences(Context.MODE_PRIVATE)
//        val username = sharedPref?.getString("username", "")
//        val token = sharedPref?.getString("token", "")
//
//        val view: View = inflater.inflate(R.layout.fragment_friend_add, container, false)
//        val submitBtn = view.findViewById<Button>(R.id.submitRequestButton)
//        val usernameSearch = view.findViewById<EditText>(R.id.editTextSearch)
//
//        val submitSearch = usernameSearch.text
//        var apiRequest : String =
//            "https://rememri-instance-5obwaiol5q-ue.a.run.app/friend_request"
//        var url : URL = URL(apiRequest)
//        request.put("user1", username)
//        request.put("user2", submitSearch.toString())
//        request.put("action", "request")
//
//        val requestString = request.toString()
//
//        Toast.makeText(activity, "JSON created", Toast.LENGTH_LONG)
//            .show()
//
//        var urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
//        urlConnection.requestMethod = "POST"
//        urlConnection.setRequestProperty("Content-Type", "application/json")
//        urlConnection.setRequestProperty("Accept", "application/json")
//        urlConnection.setRequestProperty("Authorization", "Bearer $token")
//        urlConnection.doOutput = true
//        urlConnection.doInput = true
//
//        val policy = StrictMode.ThreadPolicy.Builder()
//            .permitAll().build()
//        StrictMode.setThreadPolicy(policy)
//        val outputSt = OutputStreamWriter(urlConnection.outputStream)
//        outputSt.write(requestString)
//        outputSt.flush()
//
//        val response = urlConnection.responseCode
//        val myResp = urlConnection.responseMessage
//        if (response == HttpURLConnection.HTTP_OK) {
//            Toast.makeText(activity, "Friend successfully added", Toast.LENGTH_LONG)
//                .show()
//        } else if (response == HttpURLConnection.HTTP_BAD_REQUEST) {
//            Toast.makeText(activity, myResp.toString(), Toast.LENGTH_LONG)
//                .show()
//        } else if (response == HttpURLConnection.HTTP_CONFLICT) {
//            Toast.makeText(activity, "requested user doesn't exist", Toast.LENGTH_LONG)
//                .show()
//        } else {
//            Log.e("HTTPURLCONNECTION_ERROR", response.toString())
//            Toast.makeText(activity, "Connection Error", Toast.LENGTH_LONG)
//                .show()
//        }
//    }

}
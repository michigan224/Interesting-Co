package edu.umich.interestingco.rememri

import android.app.Activity
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private lateinit var url: URL
private lateinit var urlConnection: HttpURLConnection
private lateinit var credentials: JSONObject

fun getPins(latitude: Double, longitude: Double, activity: Activity): List<Memri>? {
    val oldSharedPref = activity.getSharedPreferences("mypref", 0)
    val token: String? = oldSharedPref.getString("token", "")
    val username: String? = oldSharedPref.getString("username", "")

    if (token !== "" && username !== "") {
        Log.d("GET_NEARBY_PINS", "Finding pins for $username")
        url = URL("https://rememri-instance-5obwaiol5q-ue.a.run.app/nearby_pins?username=$username&current_location=$latitude,$longitude")

        urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("Accept", "application/json")
        urlConnection.setRequestProperty("Authorization","Bearer $token")

        val policy = StrictMode.ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val response = urlConnection.responseCode
        if (response == HttpURLConnection.HTTP_OK) {
            val data = urlConnection.inputStream.bufferedReader().readText()
            val gson = Gson()
            val typeToken = object : TypeToken<List<Memri>>() {}.type
            val pins = gson.fromJson<List<Memri>>(data, typeToken)
            Log.d("RESPONSE", "Successfully recieved nearby pins")
            return pins
        } else {
            val data = urlConnection.inputStream.bufferedReader().readText()
            Log.e("HTTPURLCONNECTION_ERROR", data)
        }
    } else {
        Log.d("GET_NEARBY_PINS", "Finding pins for $username")
        url = URL("https://rememri-instance-5obwaiol5q-ue.a.run.app/nearby_pins?current_location=$latitude,$longitude")

        urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("Accept", "application/json")

        val policy = StrictMode.ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val response = urlConnection.responseCode
        if (response == HttpURLConnection.HTTP_OK) {
            val data = urlConnection.inputStream.bufferedReader().readText()
            val gson = Gson()
            val typeToken = object : TypeToken<List<Memri>>() {}.type
            val pins = gson.fromJson<List<Memri>>(data, typeToken)
            Log.d("RESPONSE", "Successfully recieved nearby pins")
            return pins
        } else {
            val data = urlConnection.inputStream.bufferedReader().readText()
            Log.e("HTTPURLCONNECTION_ERROR", data)
        }
    }
    return null
}
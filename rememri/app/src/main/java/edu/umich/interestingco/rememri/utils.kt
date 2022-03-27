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
    var token: String? = oldSharedPref.getString("token", "")
    var username: String? = oldSharedPref.getString("username", "")
    token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InVzZXI0In0.WB-ef6dhAF9vlvQOtyuU6F5kHq2hisM_C2o72VU5MoE"
    username = "user4"

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
        Toast.makeText(activity, username, Toast.LENGTH_LONG).show()

        //Validate sign in!!!
        url = URL("https://rememri-instance-5obwaiol5q-ue.a.run.app/accounts/sign_in")
        credentials = JSONObject()
        credentials.put("username", username)
        credentials.put("password", token)

        val credentialsString = credentials.toString()

        //val values = mapOf("username" to submitUsername, "password" to submitPassword)

        //val objectMapper = ObjectMapper()
        //val requestBody: String = objectMapper.writeValueAsString(values)

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
        outputSt.write(credentialsString)
        outputSt.flush()

        val response = urlConnection.responseCode
        if (response == HttpURLConnection.HTTP_OK) {
            val data = urlConnection.inputStream.bufferedReader().readText()
            val gson = Gson()
            val resp = gson.fromJson(data, LoginResponse.SuccessfulLoginResp::class.java)
            val token = resp.token
            Log.d("TOKEN", token)

            val sharedPref = activity.getSharedPreferences("mypref", 0)
            val editor = sharedPref.edit()
            editor.putString("token", token)
            editor.apply()
            //withContext(Dispatchers.Main)
            // val myResponse = urlConnection.inputStream.bufferedReader().use { it.readText() }
            // val gson = GsonBuilder().setPrettyPrinting().create()
            // val myJson = gson.toJson(JsonParser.parseString(myResponse))
            // Log.d("JSON :", myJson)
        } else {
            Log.e("HTTPURLCONNECTION_ERROR", response.toString())
        }
    }
    return null
}
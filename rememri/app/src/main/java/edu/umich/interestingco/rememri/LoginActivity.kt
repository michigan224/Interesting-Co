package edu.umich.interestingco.rememri

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


private lateinit var url: URL
private lateinit var urlConnection: HttpURLConnection
private lateinit var credentials: JSONObject

class LoginResponse {
    data class SuccessfulLoginResp(
        val message: String,
        val token: String
    )
}

class LoginActivity :  AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username = findViewById<EditText>(R.id.editTextTextMultiLine)
        val password = findViewById<EditText>(R.id.editTextTextPassword)
        val submitBtn = findViewById<Button>(R.id.submitButton)

        submitBtn.setOnClickListener {
            val submitUsername = username.text
            val submitPassword = password.text

            Toast.makeText(this@LoginActivity, submitUsername, Toast.LENGTH_LONG).show()

            //Validate sign in!!!
            url = URL("https://rememri-instance-5obwaiol5q-ue.a.run.app/accounts/sign_in")
            credentials = JSONObject()
            credentials.put("username", submitUsername)
            credentials.put("password", submitPassword)

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

            val policy = ThreadPolicy.Builder()
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
                //withContext(Dispatchers.Main)
                // val myResponse = urlConnection.inputStream.bufferedReader().use { it.readText() }
                // val gson = GsonBuilder().setPrettyPrinting().create()
                // val myJson = gson.toJson(JsonParser.parseString(myResponse))
                // Log.d("JSON :", myJson)
            } else {
                Log.e("HTTPURLCONNECTION_ERROR", response.toString())
            }

            //var output : DataOutputStream = urlConnection.outputStream as DataOutputStream
            //output.writeBytes("PostData=" + credentials)



           // try {
            //    val input : InputStream = BufferedInputStream(urlConnection.getInputStream())
            //    readStream(input)
           // } finally {
            //    urlConnection.disconnect()
            //}
        }
    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))
}


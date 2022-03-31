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
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private lateinit var url: URL
private lateinit var urlConnection: HttpURLConnection
private lateinit var credentials: JSONObject


// var passUsername: String? = ""

class SignupResponse {
    data class SuccessfulSignupResp(
        val message: String,
        val token: String
    )
}

class SignupActivity :  AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val myIntent: Intent = intent
        // passUsername = myIntent.getStringExtra("username_key")

        // if (passUsername != "") fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java).putExtra("username_key", passUsername))

        setContentView(R.layout.activity_signup)

        val username = findViewById<EditText>(R.id.editTextTextMultiLine)
        val password = findViewById<EditText>(R.id.editTextTextPassword)
        val submitBtn = findViewById<Button>(R.id.submitButton)

        submitBtn.setOnClickListener {
            val submitUsername = username.text
            val submitPassword = password.text

            val oldSharedPref = getSharedPreferences("mypref", 0)
            val token = oldSharedPref.getString("token", "")
            // token = "" // Uncomment this to ignore saved token

            if (token !== "") {
                Log.d("TOKEN", "Token already found")
                Toast.makeText(this@SignupActivity, "USER ALREADY LOGGED IN", Toast.LENGTH_LONG)
                    .show()
                fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))
            } else {

                Toast.makeText(this@SignupActivity, submitUsername, Toast.LENGTH_LONG).show()

                //Validate sign in!!!
                url = URL("https://rememri-instance-5obwaiol5q-ue.a.run.app/accounts/sign_up")
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
                    val resp = gson.fromJson(data, SignupResponse.SuccessfulSignupResp::class.java)
                    val token = resp.token
                    Log.d("TOKEN", token)

                    val sharedPref = getSharedPreferences("mypref", 0)
                    val editor = sharedPref.edit()
                    editor.putString("token", token)
                    editor.putString("username", submitUsername.toString())
                    editor.apply()

                    // passUsername = submitUsername.toString()
                    startActivity(Intent(this, AccountActivity::class.java))
                } else if (response == HttpURLConnection.HTTP_CONFLICT) {
                    Toast.makeText(this@SignupActivity, "Username already exists", Toast.LENGTH_LONG)
                        .show()
                } else {
                    Log.e("HTTPURLCONNECTION_ERROR", response.toString())
                }
            }
        }
    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))
}


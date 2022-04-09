package edu.umich.interestingco.rememri

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.llollox.androidtoggleswitch.widgets.ToggleSwitch
import com.squareup.picasso.Picasso
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class PostViewActivity : AppCompatActivity()  {
    private var isPublic : Boolean = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_view_activity)
        val myIntent = intent.extras
        val mimageView = findViewById<ImageView>(R.id.imageCard)
        val sharedPref : SharedPreferences?= getSharedPreferences("mypref", Context.MODE_PRIVATE)
        val username = sharedPref?.getString("username", "")
        val token = sharedPref?.getString("token", "")
        val myImage = Uri.parse(myIntent?.getString("media_url"))

        Picasso.get().load(myImage).into(mimageView)
//        val imageRounded = Bitmap.createBitmap(mbitmap.width, mbitmap.height, mbitmap.config)
//        val canvas = Canvas(imageRounded)
//        val mpaint = Paint()
//        mpaint.isAntiAlias = true
//        mpaint.shader = BitmapShader(mbitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
//        canvas.drawRoundRect(
//            RectF(0F, 0F, mbitmap.width.toFloat(), mbitmap.height.toFloat()),
//            100f,
//            100f,
//            mpaint
//        ) // Round Image Corner 100 100 100 100
//        mimageView.setImageBitmap(imageRounded)

        val mySwitch : ToggleSwitch = findViewById(R.id.switchPublic)
        mySwitch.checkedPosition = 0

        mySwitch.onChangeListener = object : ToggleSwitch.OnChangeListener {
            override fun onToggleSwitchChanged(position: Int) {
                Log.d("DEBUG", "position changed to $position")

                //Friend Position
                isPublic = position == 1
            }
        }

        val submit : Button = findViewById(R.id.commentButton)

        submit.setOnClickListener {
            val apiRequest =
                "https://rememri-instance-5obwaiol5q-ue.a.run.app/pin"
            val url = URL(apiRequest)

            val request : JSONObject = JSONObject()
            request.put("username", username)
            request.put("is_public", isPublic)
            request.put("pin_location", myIntent?.getString("pin_location"))
            request.put("image", myImage.toString())

            val requestString = request.toString()

            val urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
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
                Toast.makeText(this, "Pin Posted!", Toast.LENGTH_LONG)
                    .show()
                val newIntent = Intent(this, MainActivity::class.java)
                startActivity(newIntent)
            } else if (response == HttpURLConnection.HTTP_CONFLICT) {
                Toast.makeText(this, "requested user doesn't exist", Toast.LENGTH_LONG)
                    .show()
            } else {
                Log.e("HTTPURLCONNECTION_ERROR", response.toString())
                Toast.makeText(this, "Connection Error", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

}
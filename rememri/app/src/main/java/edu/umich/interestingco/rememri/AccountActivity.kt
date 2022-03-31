package edu.umich.interestingco.rememri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView

class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oldSharedPref = getSharedPreferences("mypref", 0)
        val username = oldSharedPref.getString("username", "")
        val token = oldSharedPref.getString("token", "")
        setContentView(R.layout.activity_account)

        if (token == ""){
            val accountName = findViewById<TextView>(R.id.textView)
            accountName.text = "guest"
        }
        else {
            val accountName = findViewById<TextView>(R.id.textView)
            accountName.text = username
            val signUpButton = findViewById<TextView>(R.id.signUp)
            val loginButton = findViewById<TextView>(R.id.loginButton)
            val logoutButton = findViewById<TextView>(R.id.logoutButton)

            signUpButton.visibility = android.view.View.GONE
            loginButton.visibility = android.view.View.GONE
            logoutButton.visibility = android.view.View.VISIBLE

            logoutButton.setOnClickListener {
                val sharedPref = getSharedPreferences("mypref", 0)
                val editor = sharedPref.edit()
                editor.remove("token")
                editor.remove("username")
                editor.apply()

                fun returnLogin(view: View?) = startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    fun returnSignup(view: View?) = startActivity(Intent(this, SignupActivity::class.java))

    fun returnLogin(view: View?) = startActivity(Intent(this, LoginActivity::class.java))
}


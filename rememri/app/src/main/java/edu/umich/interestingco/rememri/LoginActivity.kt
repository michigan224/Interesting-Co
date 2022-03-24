package edu.umich.interestingco.rememri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LoginActivity :  AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var username = findViewById<EditText>(R.id.editTextTextMultiLine)
        var password = findViewById<EditText>(R.id.editTextTextPassword)
        var submit_btn = findViewById<Button>(R.id.submitButton)

        submit_btn.setOnClickListener {
            val submit_username = username.text
            val submit_password = password.text

            Toast.makeText(this@LoginActivity, submit_username, Toast.LENGTH_LONG).show()

            //Validate sign in!!!
        }
    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))
}


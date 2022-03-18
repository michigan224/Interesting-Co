package edu.umich.interestingco.rememri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class AccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        print("made is this far")
        setContentView(R.layout.activity_account)
    }

    fun returnMain(view: View?) = startActivity(Intent(this, MainActivity::class.java))

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    fun returnSignup(view: View?) = startActivity(Intent(this, SignupActivity::class.java))

    fun returnLogin(view: View?) = startActivity(Intent(this, LoginActivity::class.java))
}


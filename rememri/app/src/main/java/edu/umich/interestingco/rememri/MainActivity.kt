package edu.umich.interestingco.rememri

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import edu.umich.interestingco.rememri.databinding.ActivityMainBinding
import android.content.ComponentName
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {
    private lateinit var view: ActivityMainBinding
    private var enableSend = true
    private val viewState: ImageViewState by viewModels()
    private lateinit var forCropResult: ActivityResultLauncher<Intent>
    private lateinit var forCameraButton: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)
    }

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))
}
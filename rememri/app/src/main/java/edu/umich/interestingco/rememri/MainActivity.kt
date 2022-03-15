package edu.umich.interestingco.rememri

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import edu.umich.interestingco.rememri.databinding.ActivityPostBinding

import android.app.usage.UsageEvents.Event.NONE
import android.content.ComponentName
import android.content.ContentValues
import android.icu.text.Collator.ReorderCodes.FIRST
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import androidx.activity.viewModels

class MainActivity : AppCompatActivity() {
    private lateinit var view: ActivityPostBinding
    private var enableSend = true
    private val viewState: ImageViewState by viewModels()
    private lateinit var forCropResult: ActivityResultLauncher<Intent>
    private lateinit var forCameraButton: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityPostBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)

        // Get the permissions set up
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach {
                if (!it.value) {
                    toast("${it.key} access denied")
                    finish()
                }
            }
        }.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE))

        val cropIntent = initCropIntent()
        forCropResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data.let {
                        viewState.imageUri?.run {
                            if (!toString().contains("ORIGINAL")) {
                                // delete uncropped photo taken for posting
                                contentResolver.delete(this, null, null)
                            }
                        }
                        viewState.imageUri = it
                    }
                } else {
                    Log.d("Crop", result.resultCode.toString())
                }
            }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            toast("Device has no camera!")
            return
        }

        forCameraButton =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    doCrop(cropIntent)
                } else {
                    Log.d("TakePicture", "failed")
                }
            }

        // TODO : This is some real sketchy shit that probabbly wont work.... <3 oh well
        findViewById<ImageButton>(R.id.cameraButton).setOnClickListener {
            viewState.imageUri = mediaStoreAlloc("image/jpeg")
            forCameraButton.launch(viewState.imageUri)
        }
    }

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    private fun initCropIntent(): Intent? {
        // Is there any published Activity on device to do image cropping?
        val intent = Intent("com.android.camera.action.CROP")
        intent.type = "image/*"
        val listofCroppers = packageManager.queryIntentActivities(intent, 0)
        // No image cropping Activity published
        if (listofCroppers.size == 0) {
            toast("Device does not support image cropping")
            return null
        }

        intent.component = ComponentName(
            listofCroppers[0].activityInfo.packageName,
            listofCroppers[0].activityInfo.name)

        // create a square crop box:
        intent.putExtra("outputX", 500)
            .putExtra("outputY", 500)
            .putExtra("aspectX", 1)
            .putExtra("aspectY", 1)
            // enable zoom and crop
            .putExtra("scale", true)
            .putExtra("crop", true)
            .putExtra("return-data", true)

        return intent
    }

    private fun doCrop(intent: Intent?) {
        intent ?: run {
            return
        }

        viewState.imageUri?.let {
            intent.data = it
            forCropResult.launch(intent)
        }
    }

    private fun mediaStoreAlloc(mediaType: String): Uri? {
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.MIME_TYPE, mediaType)
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)

        return contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
    }
}

class ImageViewState: ViewModel() {
    var enableSend = true
    var imageUri: Uri? = null
}
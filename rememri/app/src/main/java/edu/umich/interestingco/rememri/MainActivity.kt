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
import edu.umich.interestingco.rememri.databinding.ActivityMainBinding
import android.content.ComponentName
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageButton
import com.mapbox.maps.MapView
import com.mapbox.maps.Style

// From Mapbox -->
//import com.mapbox.maps.Style

var mapView: MapView? = null

class MainActivity : AppCompatActivity() {
    private lateinit var view: ActivityMainBinding
    private val viewState: ImageViewState by viewModels()
    private lateinit var forCropResult: ActivityResultLauncher<Intent>
    private lateinit var forCameraButton: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMainBinding.inflate(layoutInflater)
        mapView = findViewById(R.id.mapView)

        setContentView(R.layout.activity_main)

        mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS)

        // Initializing is asynchrounous- getMapAsync will return a map
       // mapView?.get { map ->
            // Set one of the many styles available
            //map.setStyle(Style.OUTDOORS) { style ->
             //   Style.MAPBOX_STREETS
            //}
        //}

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

        // For Mapbox -->
        //mapView = findViewById(R.id.mapView)
        //mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS)

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

        // TODO : This might be wrong, it works rn but idk, it worries me
        findViewById<ImageButton>(R.id.cameraButton).setOnClickListener {
            viewState.imageUri = mediaStoreAlloc("image/jpeg")
            forCameraButton.launch(viewState.imageUri)
        }
    }

    // Needed for Mapbox
//    override fun onStart() {
//        super.onStart()
//        mapView?.onStart()
//    }
//
//    // Needed for Mapbox
//    override fun onStop() {
//        super.onStop()
//        mapView?.onStop()
//    }
//
//    // Needed for Mapbox
//    override fun onLowMemory() {
//        super.onLowMemory()
//        mapView?.onLowMemory()
//    }
//
//    // Needed for Mapbox
//    override fun onDestroy() {
//        super.onDestroy()
//        mapView?.onDestroy()
//    }

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
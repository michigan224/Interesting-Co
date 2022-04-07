package edu.umich.interestingco.rememri

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.llollox.androidtoggleswitch.widgets.ToggleSwitch
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import edu.umich.interestingco.rememri.databinding.ActivityMainBinding
import java.lang.ref.WeakReference


var mapView: MapView? = null

class MainActivity : AppCompatActivity() {
    private lateinit var view: ActivityMainBinding
    private val viewState: ImageViewState by viewModels()
    private lateinit var forCropResult: ActivityResultLauncher<Intent>
    private lateinit var forCameraButton: ActivityResultLauncher<Uri>
    private var fusedLocationClient: FusedLocationProviderClient? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)

        // Center the map on the user's location
        mapView = findViewById(R.id.mapView)
        centerMapOnUserLocation(mapView)

        // Set position of public/private switch to PUBLIC
        // [PUBLIC = 0 | PRIVATE = 1]
        var publicPrivateSwitch: ToggleSwitch? = null
        publicPrivateSwitch = findViewById(R.id.public_private_switch)
        publicPrivateSwitch.setCheckedPosition(0)

        Log.d("TOGGLE_SWITCH", "TEST")

//        publicPrivateSwitch.setOnClickListener { object : ToggleSwitch.OnChangeListener {
//            override fun onToggleSwitchChanged(position: Int) {
//                Log.d("TOGGLE_SWITCH", "Toggle Switch Clicked")
//
//                if (publicPrivateSwitch.getCheckedPosition() == 0){
//                    Log.d("TOGGLE_SWITCH", "Switch is Public")
//                }
//                else if (publicPrivateSwitch.getCheckedPosition() == 1){
//                    Log.d("TOGGLE_SWITCH", "Switch is Private")
//                }
//            }
//        }}

        publicPrivateSwitch.setOnClickListener {
            Log.d("TOGGLE_SWITCH", "Toggle Switch Clicked")

            if (publicPrivateSwitch.getCheckedPosition() == 0){
                Log.d("TOGGLE_SWITCH", "Switch is Public")
            }
            else if (publicPrivateSwitch.getCheckedPosition() == 1){
                Log.d("TOGGLE_SWITCH", "Switch is Private")
            }
        }


//        findViewById<ToggleSwitch>(R.id.public_private_switch)

        /*
        *
        * findViewById<ImageButton>(R.id.cameraButton).setOnClickListener {
            viewState.imageUri = mediaStoreAlloc("image/jpeg")
            forCameraButton.launch(viewState.imageUri)
        }
        * */

        // Get the permissions set up
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach {
                if (!it.value) {
                    toast("${it.key} access denied")
                    finish()
                }
            }
        }.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )

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
    } // onCreate [END]

    //---------------------------------MAPBOX--------------------------------//
    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView?.getMapboxMap()?.setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView?.getMapboxMap()?.setCamera(CameraOptions.Builder().center(it).build())
        mapView?.gestures?.focalPoint = mapView?.getMapboxMap()?.pixelForCoordinate(it)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }
//    private lateinit var mapView: MapView

    private fun setupGesturesListener() {
        mapView?.gestures?.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView?.location
//        locationComponentPlugin?.updateSettings {
//            this.enabled = true
//            this.locationPuck = LocationPuck2D(
//                bearingImage = AppCompatResources.getDrawable(
//                    this@MainActivity,
//                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_puck_icon,
//                ),
//                shadowImage = AppCompatResources.getDrawable(
//                    this@MainActivity,
//                    com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon_shadow,
//                ),
//                scaleExpression = interpolate {
//                    linear()
//                    zoom()
//                    stop {
//                        literal(0.0)
//                        literal(0.6)
//                    }
//                    stop {
//                        literal(20.0)
//                        literal(1.0)
//                    }
//                }.toJson()
//            )
//        }
        locationComponentPlugin?.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
            topImage = AppCompatResources.getDrawable(
                this@MainActivity,
                com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_icon
            ),
            bearingImage = AppCompatResources.getDrawable(
                this@MainActivity,
                com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_bearing_icon
            ),
            shadowImage = AppCompatResources.getDrawable(
                this@MainActivity,
                com.mapbox.maps.plugin.locationcomponent.R.drawable.mapbox_user_stroke_icon
            ),
            scaleExpression = interpolate {
                linear()
                zoom()
                stop {
                    literal(0.0)
                    literal(0.6)
                }
                stop {
                    literal(20.0)
                    literal(1.0)
                }
            }.toJson()
        )
        }
        locationComponentPlugin?.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin?.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }

    private fun onCameraTrackingDismissed() {
        // Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView?.location
            ?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView?.location
            ?.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView?.gestures?.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.location
            ?.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView?.location
            ?.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView?.gestures?.removeOnMoveListener(onMoveListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun addAnnotationToMap(latitude: Double, longitude: Double) {
        // Create an instance of the Annotation API and get the PointAnnotationManager.
        bitmapFromDrawableRes(
            this@MainActivity,
            R.drawable.red_marker
        )?.let {
            val annotationApi = mapView?.annotations
            val pointAnnotationManager = annotationApi?.createPointAnnotationManager(mapView!!)
            // Set options for the resulting symbol layer.
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            // Define a geographic coordinate.
                .withPoint(Point.fromLngLat(longitude, latitude))
            // Specify the bitmap you assigned to the point annotation
            // The bitmap will be added to map style automatically.
                .withIconImage(it)
            // Add the resulting pointAnnotation to the map.
            pointAnnotationManager?.create(pointAnnotationOptions)
        }
    }
    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    //---------------------------------MAPBOX--------------------------------//

    fun returnFriends(view: View?) = startActivity(Intent(this, FriendActivity::class.java))

    fun returnAccount(view: View?) = startActivity(Intent(this, AccountActivity::class.java))

    fun returnAR(view: View?) = startActivity(Intent(this, ARView::class.java))

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

    fun centerMapOnUserLocation(view: View?){
        mapView = findViewById(R.id.mapView)
        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
        locationPermissionHelper.checkPermissions {
            mapView?.getMapboxMap()?.setCamera(
                CameraOptions.Builder()
                    .zoom(14.0)
                    .build()
            )
            mapView?.getMapboxMap()?.loadStyleUri(
                Style.MAPBOX_STREETS
            )
            {
                initLocationComponent()
                setupGesturesListener()
                // addAnnotationToMap(42.292083,-83.71588)
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful && task.result != null) {
                        getPins(task.result!!.latitude, task.result!!.longitude, this)?.forEach(
                            (fun(memri: Memri){
                                addAnnotationToMap((memri.location?.get(0) ?: 0) as Double,(memri.location?.get(1) ?: 0) as Double)
                            })
                        )
                    } else {
                        Log.w("ERROR", "getLastLocation:exception", task.exception)
                    }
                }
            }
        }
    }

    // Set 2D map view to PUBLIC or PRIVATE
    // [PUBLIC = 0 | PRIVATE = 1]
    fun setPublicPrivateView(view: View?){
        var publicPrivateSwitch: ToggleSwitch? = null
        publicPrivateSwitch = findViewById(R.id.public_private_switch)

        val pos = publicPrivateSwitch.getCheckedPosition()

        // If the switch is set to PUBLIC
        if (pos == 0){
            Log.d("MAP_SWITCH", "Switch is public")
        }
        // If the switch is set to PRIVATE
        else if (pos == 1) {
            Log.d("MAP_SWITCH", "Switch is private")
        }
    }
}

class ImageViewState: ViewModel() {
    var enableSend = true
    var imageUri: Uri? = null
}
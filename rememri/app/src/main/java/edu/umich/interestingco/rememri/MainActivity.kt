package edu.umich.interestingco.rememri

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
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
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import edu.umich.interestingco.rememri.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
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

        val oldSharedPref = getSharedPreferences("mypref", 0)
        val token = oldSharedPref.getString("token", "")

        // Set position of public/private switch to PUBLIC
        // [PUBLIC = 0 | FRIENDS ONLY = 1]
        val publicPrivateSwitch: ToggleSwitch = findViewById(R.id.public_private_switch)
        publicPrivateSwitch.setCheckedPosition(0)

        val loginIntent: Intent = Intent(this, AccountActivity::class.java)

        publicPrivateSwitch.onChangeListener = object : ToggleSwitch.OnChangeListener {
            override fun onToggleSwitchChanged(position: Int) {
                Log.d("DEBUG", "position changed to $position")

                if (token == "" && position == 1) {
                    Toast.makeText(this@MainActivity, "Not Logged In", Toast.LENGTH_LONG)
                        .show()
                    startActivity(loginIntent)
                }

                getFilteredPins()
            }
        }

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
                        var filePath: String? = null
                        val _uri: Uri? = it
                        viewState.imageUri = it
                        Log.d("", "URI = $_uri")
                        if (_uri != null && "content" == _uri.scheme) {
                            val cursor: Cursor? = this.contentResolver.query(
                                _uri,
                                arrayOf(MediaStore.Images.ImageColumns.DATA),
                                null,
                                null,
                                null
                            )
                            cursor?.moveToFirst()
                            filePath = cursor?.getString(0)
                            cursor?.close()
                        } else {
                            filePath = _uri!!.path
                        }
                        Log.d("", "Chosen path = $filePath")
                        val bm = BitmapFactory.decodeFile(filePath)
                        val baos = ByteArrayOutputStream()
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos) // bm is the bitmap object
                        val b: ByteArray = baos.toByteArray()
                        val encodedImage: String = Base64.encodeToString(b, Base64.DEFAULT)

                        val postViewIntent: Intent = Intent(this, PinAddActivity::class.java)

                        // Get media url for the new image
                        postViewIntent.putExtra("raw_media_url", viewState.imageUri.toString())
                        postViewIntent.putExtra("media_url", encodedImage)
                        Log.d("DEBUG", "added image URI to postViewIntent --> ${viewState.imageUri}")

                        // Get user's current location using the phone location
                        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                        fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
                            if (task.isSuccessful && task.result != null) {
                                val coordArray: Array<Double> = arrayOf(task.result!!.latitude, task.result!!.longitude)
                                postViewIntent.putExtra("pin_location", coordArray)
                                Log.d("DEBUG", "added pin location to postViewIntent --> $coordArray")
                                // Start Post View Activity with new added info
                                startActivity(postViewIntent)
                            } else {
                                Log.w("ERROR", "getLastLocation:exception", task.exception)
                            }
                        }
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

    private fun addAnnotationToMap(latitude: Double, longitude: Double, post_id: String) {
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
            pointAnnotationManager?.addClickListener(OnPointAnnotationClickListener {
                val intent = Intent(this, PinViewActivity::class.java)
                intent.putExtra("post_id", post_id)
                setContentView(R.layout.activity_post_ar)
                startActivity(intent)
                true
            })
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
                getFilteredPins()
            }
        }
    }

    // Filter Pins based on the position of the toggle switch
    // [PUBLIC = 0 | FRIENDS = 1]
    private fun getFilteredPins(){
        val publicPrivateSwitch: ToggleSwitch = findViewById(R.id.public_private_switch)
        val switchPosition: Int = publicPrivateSwitch.getCheckedPosition()

        // disregard error, this function only gets called once permissions have been dealt with
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                // If the 2D Map view is set to FRIENDS ONLY
                if (switchPosition == 1){
                    getPins(task.result!!.latitude, task.result!!.longitude, this)?.forEach(
                        (fun(memri: Memri) {
                            if(memri.is_friend == true){
                                addAnnotationToMap(
                                    (memri.location?.get(0) ?: 0) as Double,
                                    (memri.location?.get(1) ?: 0) as Double,
                                    memri.pin_id as String
                                )
                            }
                        })
                    )
                }
                // If the 2D Map View is set to PUBLIC
                else {
                    getPins(task.result!!.latitude, task.result!!.longitude, this)?.forEach(
                        (fun(memri: Memri) {
                            if(memri.is_public == true){
                                addAnnotationToMap(
                                    (memri.location?.get(0) ?: 0) as Double,
                                    (memri.location?.get(1) ?: 0) as Double,
                                    memri.pin_id as String
                                )
                            }
                        })
                    )
                }
            } else {
                Log.w("ERROR", "getLastLocation:exception", task.exception)
            }
        }
    }
}

class ImageViewState: ViewModel() {
    var enableSend = true
    var imageUri: Uri? = null
}
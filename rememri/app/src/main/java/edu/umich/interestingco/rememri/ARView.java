package edu.umich.interestingco.rememri;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import android.os.Handler;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import edu.umich.interestingco.rememri.R;
import android.view.ViewGroup;
import java.util.ArrayList;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import org.json.*;
import java.io.*;
import java.net.*;
import android.os.StrictMode;
import android.util.Base64;
import java.util.List;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

public class ARView extends AppCompatActivity{
    private static final String TAG = ARView.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private ImageView image_view;

    private double latitude;
    private double longitude;
    private double prev_latitude;
    private double prev_longitude;
    private boolean updatedLocation = true;
    private List<ImageView> imageViewList = new ArrayList<ImageView>();
    private LocationManager locationManager;
    private LocationListener locationListener;


    private ViewRenderable imageRenderable;
    private boolean hasFinishedLoading = false;
    ArrayList<AnchorNode> node_list = new ArrayList<AnchorNode>();

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ar);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        CompletableFuture<ViewRenderable> imageStage =
                ViewRenderable.builder().setView(this, R.layout.image_view).build();

        CompletableFuture.allOf(
                imageStage)
                .handle(
                        (notUsed, throwable) -> {
                            if (throwable != null) {
                                return null;
                            }
                            try {
                                imageRenderable = imageStage.get();
                                hasFinishedLoading = true;
                            } catch (InterruptedException | ExecutionException ex) {
                            }
                            return null;
                        });

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        float min_dist_to_update = 1;
        long min_update_time = 1000;
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);

        if (locationManager != null) {
            Location location = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                prev_latitude = latitude;
                prev_longitude = longitude;
            }
        }


        final Handler handler = new Handler();
        final int delay = 10000; // 1000 milliseconds == 1 second

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
                double lat_diff = Math.pow(prev_latitude - latitude, 2);
                double long_diff = Math.pow(prev_longitude - longitude, 2);
                double total_diff = Math.sqrt(lat_diff + long_diff);
//                Toast.makeText(getApplicationContext(), "Diff: " + String.valueOf(total_diff), Toast.LENGTH_SHORT).show();
                if (total_diff > 3E-4) {
                    updatedLocation = true;
                    prev_latitude = latitude;
                    prev_longitude = longitude;
                }
                if (updatedLocation) {
                    Toast.makeText(getApplicationContext(), "Updated to New Location", Toast.LENGTH_LONG).show();
                    UpdateImages();
                    DisplayImage();
                    updatedLocation = false;
                }
                handler.postDelayed(this, delay);
            }
        }, 5000);
    }

    public void UpdateImages() {
        imageViewList = new ArrayList<ImageView>();
        try{
            JSONObject jsonObject = getJSONObjectFromURL("https://rememri-instance-5obwaiol5q-ue.a.run.app/nearby_pins?current_location=" +
                    String.valueOf(latitude) + "," + String.valueOf(longitude));
            System.out.println("Json_obj: " + jsonObject.toString());
            JSONArray jsonArray = jsonObject.getJSONArray("arr");
            for (int it = 0; it < jsonArray.length(); it++) {
                JSONObject next_image_JSON = jsonArray.getJSONObject(it);
                String next_image_string = next_image_JSON.getString("media_url");
                byte[] decodedString = Base64.decode(next_image_string, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,decodedString.length);
                ImageView i = new ImageView(this);
                i.setImageBitmap(decodedByte);
                i.setAdjustViewBounds(true);
                i.setLayoutParams(new ViewGroup.LayoutParams(
                        100 * 3,
                        200 * 3));
                imageViewList.add(i);
                System.out.println(next_image_string);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("JSON Response", "Error");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("JSON Response", "Error");
        }
    }

    public void DisplayImage() {
        for (AnchorNode e : node_list){
            arFragment.getArSceneView().getScene().removeChild(e);
        }

        float j = 0;
        for (ImageView element : imageViewList) {

            CompletableFuture<ViewRenderable> imageStage =
                    ViewRenderable.builder().setView(this, element).build();
            float finalJ = j;
            CompletableFuture.allOf(
                    imageStage)
                    .handle(
                            (notUsed, throwable) -> {

                                if (throwable != null) {
                                    return null;
                                }

                                try {
                                    Session session = arFragment.getArSceneView().getSession();
                                    float[] camera_pos = arFragment.getArSceneView().getArFrame().getCamera().getPose().getTranslation();
                                    camera_pos[0] += finalJ * 0.5f - finalJ;
                                    camera_pos[2] -= 1;
                                    float[] rotation = { 0, 0, 0, 1 };
                                    Anchor anchor =  session.createAnchor(new Pose(camera_pos, rotation));
                                    AnchorNode anchorNode = new AnchorNode(anchor);
                                    anchorNode.setRenderable(imageStage.get());
                                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                                    node_list.add(anchorNode);
                                } catch (InterruptedException | ExecutionException ex) {
                                }

                                return null;
                            });
            j += 1;
        }
    }

    // JSON request intepreter code from https://stackoverflow.com/questions/34691175/how-to-send-httprequest-and-get-json-response-in-android
    public static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        URL url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000);
        urlConnection.setConnectTimeout(15000);
        urlConnection.setDoOutput(true);
        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        String jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);
        jsonString = "{\"arr\": " + jsonString + "}";
        JSONObject return_JSON = new JSONObject((jsonString));

        return new JSONObject(jsonString);
    }
    
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
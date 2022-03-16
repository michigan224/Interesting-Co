package edu.umich.interestingco.rememri;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
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

public class ARView extends AppCompatActivity {
    private static final String TAG = ARView.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private ImageView image_view;

    private ViewRenderable imageRenderable;
    private boolean hasFinishedLoading = false;

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

                                // Everything finished loading successfully.
                                hasFinishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                            }

                            return null;
                        });

        final Handler handler = new Handler();
        final int delay = 10000; // 1000 milliseconds == 1 second

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DisplayImage();
                handler.postDelayed(this, delay);
            }
        }, 5000);
    }

    public void DisplayImage() {
        ImageView i = new ImageView(this);
        i.setImageResource(R.drawable.goat);
        i.setAdjustViewBounds(true);
        i.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        CompletableFuture<ViewRenderable> imageStage =
                ViewRenderable.builder().setView(this, i).build();
        CompletableFuture.allOf(
                imageStage)
                .handle(
                        (notUsed, throwable) -> {

                            if (throwable != null) {
                                return null;
                            }

                            try {
                                Session session = arFragment.getArSceneView().getSession();
                                float[] pos = { 0, 0, -1 };
                                float[] rotation = { 0, 0, 0, 1 };
                                Anchor anchor =  session.createAnchor(new Pose(pos, rotation));
                                AnchorNode anchorNode = new AnchorNode(anchor);
                                anchorNode.setRenderable(imageStage.get());
                                anchorNode.setParent(arFragment.getArSceneView().getScene());

                            } catch (InterruptedException | ExecutionException ex) {
                            }

                            return null;
                        });
    }


    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
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
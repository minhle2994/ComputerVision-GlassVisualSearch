package minhle.com.demoglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.glass.content.Intents;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final int TAKE_VIDEO_REQUEST = 2;
    private static final int CAMERA_REQUEST = 1888;
    private GestureDetector mGestureDetector = null;
    private CameraView cameraView = null;
    private static final String API_KEY = "cNSiII2zZzS_TC9D2D0ZVw";
    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private View mView;
    private String mName="xxx";

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initiate CameraView
        cameraView = new CameraView(this);

        // Turn on Gestures
        mGestureDetector = createGestureDetector(this);

        // Set the view
        this.setContentView(cameraView);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Do not hold the camera during onResume
        if (cameraView != null) {
            cameraView.releaseCamera();
        }

        // Set the view
        this.setContentView(cameraView);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Do not hold the camera during onPause
        if (cameraView != null) {
            cameraView.releaseCamera();
        }
    }

    /**
     * Gesture detection for fingers on the Glass
     *
     * @param context
     *
     * @return
     */
    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);

        //Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                // Make sure view is initiated
                if (cameraView != null) {
                    // Tap with a single finger for photo
                    if (gesture == Gesture.TAP) {
//                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
                        File tempFile = null;
                        try {
                            tempFile = File.createTempFile("camera", ".png", getExternalCacheDir());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String path = tempFile.getAbsolutePath();
                        Uri uri = Uri.fromFile(tempFile);

                        Intent cameraIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);

                        return true;
                    }
                    // Tap with 2 fingers for video
                    else if (gesture == Gesture.TWO_TAP) {
                        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        startActivityForResult(intent, TAKE_VIDEO_REQUEST);

                        return true;
                    }
                }

                return false;
            }
        });

        return gestureDetector;
    }

    /*
     * Send generic motion events to the gesture detector
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle photos
        Log.e("CODE", Integer.toString(resultCode));
        Log.e("REQUEST CODE", Integer.toString(requestCode));
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Log.e("OK ", Integer.toString(requestCode));
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
            Bundle extras = data.getExtras();
            Bitmap photo = (Bitmap) extras.get("data");
            Intent intent = new Intent(getApplicationContext(),ShowActivity.class);
            intent.putExtra("PATH",picturePath);
            startActivity(intent);
        }

        // Handle videos
        if (requestCode == TAKE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            String picturePath = data.getStringExtra(Intents.EXTRA_VIDEO_FILE_PATH);
            Intent intent = new Intent(getApplicationContext(),ShowActivity.class);
            intent.putExtra("PATH",picturePath);
            startActivity(intent);
        }


    }



    /**
     * Added but irrelevant
     */
    /*
	 * (non-Javadoc)
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            // Stop the preview and release the camera.
            // Execute your logic as quickly as possible
            // so the capture happens quickly.
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}
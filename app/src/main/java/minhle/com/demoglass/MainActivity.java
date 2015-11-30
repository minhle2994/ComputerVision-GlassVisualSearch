package minhle.com.demoglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.content.Intents;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
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
    private GestureDetector mGestureDetector = null;
    private CameraView cameraView = null;
    private static final String API_KEY = "cNSiII2zZzS_TC9D2D0ZVw";
    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private View mView;
    private String mName;

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

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView() {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        card.setText(mName);


        return card.getView();
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
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, TAKE_PICTURE_REQUEST);

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
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            Log.e("OK ", Integer.toString(requestCode));
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
            processPictureWhenReady(picturePath);
        }

        // Handle videos
        if (requestCode == TAKE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            String picturePath = data.getStringExtra(Intents.EXTRA_VIDEO_FILE_PATH);
            processPictureWhenReady(picturePath);
        }


    }

    /**
     * Process picture - from example GDK
     *
     * @param picturePath
     */
    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);
        if (pictureFile.exists()) {
            Log.e("FILE EXIST", picturePath);
            // The picture is ready; process it.
//            uploadImage(pictureFile);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath, options);
            Bitmap resized = imageResize(bitmap, bitmap.getWidth() / 4, bitmap.getHeight() / 4);
            OutputStream stream = null;
            try {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                File file = new File(path, "resized.jpg");
                if (file.exists())
                    file.delete();
                stream = new FileOutputStream(file);
                resized.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                stream.flush();
                stream.close();
                uploadImage(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
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

    public void uploadImage(File image){
        RequestParams params = new RequestParams();
        String url;
        try {
            params.put("image_request[image]", image);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        params.put("image_request[locale]", "vi-VN");
        Log.e("Upload image: ", image.getAbsolutePath());
        final AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(500000);
//        client.setConnectTimeout(500000);
//        client.setResponseTimeout(10000);
        client.addHeader("Authorization", "CloudSight cNSiII2zZzS_TC9D2D0ZVw");
        client.post("https://api.cloudsightapi.com/image_requests", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers,
                                  org.json.JSONObject response) {
                Log.e("RESPONSE: ", response.toString());

                try {
                    Thread.sleep(4000);
                    String token = response.getString("token");
                    Log.e("TOKEN STR: ", token);

                    AsyncHttpClient client_get = new AsyncHttpClient();
                    client_get.setTimeout(500000);

                    client_get.addHeader("Authorization", "CloudSight cNSiII2zZzS_TC9D2D0ZVw");
                    client_get.get("http://api.cloudsightapi.com/image_responses/" + token, null, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers,
                                              org.json.JSONObject response) {
                            Log.e("RESPONSE: ", response.toString());

                            try {
//                                String result = response.getString("name");
                                String status = response.getString("status");
                                String name = response.getString("name");
                                mName = name;
                                mView = buildView();
                                Log.e("STATUS: ", status);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode,
                                              cz.msebera.android.httpclient.Header[] headers,
                                              java.lang.Throwable throwable,
                                              org.json.JSONObject errorResponse) {
                            // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                            Log.e("Error: ", Integer.toString(statusCode));
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode,
                                  cz.msebera.android.httpclient.Header[] headers,
                                  java.lang.Throwable throwable,
                                  org.json.JSONObject errorResponse) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                Log.e("Error: ", Integer.toString(statusCode));
                Log.e("Error: ", errorResponse.toString());
            }

            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String error, java.lang.Throwable throwable){
                Log.e("Error: ", Integer.toString(statusCode));
                Log.e("Error: ", error);
            }
        });
    }

    public Bitmap imageResize(Bitmap bitmap,int newWidth,int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    protected void getDescriptionForImageURL(String url) {
        RequestParams params = new RequestParams();
        params.put("image_request[image]", "http://channel.vcmedia.vn/thumb_w/640/prupload/441/2015/11/img20151130093735510.jpg");

        AsyncHttpClient client = new AsyncHttpClient();
        client.setConnectTimeout(500000);
        client.post("https://api.cloudsightapi.com/image_requests", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers,
                                  org.json.JSONObject response) {
                try {
                    if (response.getString("status_txt").equals("OK")) {
                        JSONObject data = response.getJSONObject("data");
                        String url = data.getString("img_url");
                        Log.e("Url: ", url);
                        getDescriptionForImageURL(url);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode,
                                  cz.msebera.android.httpclient.Header[] headers,
                                  java.lang.Throwable throwable,
                                  org.json.JSONObject errorResponse) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                try {
                    Log.e("Error: ", errorResponse.getString("status_txt"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
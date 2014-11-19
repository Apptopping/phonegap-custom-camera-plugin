
package com.performanceactive.plugins.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

public class CustomCameraActivity extends Activity {

    private static final String TAG = CustomCameraActivity.class.getSimpleName();
    public static String FILENAME = "Filename";
    public static String QUALITY = "Quality";
    public static String TARGET_WIDTH = "TargetWidth";
    public static String TARGET_HEIGHT = "TargetHeight";
    public static String IMAGE_URI = "ImageUri";
    public static String ERROR_MESSAGE = "ErrorMessage";
    public static int RESULT_ERROR = 2;

    private Camera camera;
    private RelativeLayout layout;
    private RelativeLayout cameraPreviewView;
    private ImageView overlayImageView;

    private ImageButton captureButton;

    @Override
    protected void onResume() {
        super.onResume();
        try {
            camera = Camera.open();
            configureCamera();
            displayCameraPreview();
        } catch (Exception e) {
            finishWithError("Camera is not accessible");
        }
    }

    private void configureCamera() {
        Camera.Parameters cameraSettings = camera.getParameters();
        cameraSettings.setJpegQuality(100);
        cameraSettings.set("orientation", "landscape");
        List<String> supportedFocusModes = cameraSettings.getSupportedFocusModes();
        if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
            cameraSettings.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (supportedFocusModes.contains(FOCUS_MODE_AUTO)) {
            cameraSettings.setFocusMode(FOCUS_MODE_AUTO);
        }
        cameraSettings.setFlashMode(FLASH_MODE_OFF);
        camera.setParameters(cameraSettings);
    }

    private void displayCameraPreview() {
        cameraPreviewView.removeAllViews();
        cameraPreviewView.addView(new CustomCameraPreview(this, camera));
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        layout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(layoutParams);
        createCameraPreview();
        createOverlay();
        createCaptureButton();
        setContentView(layout);
    }

    private void createCameraPreview() {
        cameraPreviewView = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
       
        layoutParams.topMargin = dpToPixels(48);
        layoutParams.bottomMargin = dpToPixels(100);
        layoutParams.leftMargin = dpToPixels(0);
        layoutParams.rightMargin = dpToPixels(0);
        
        cameraPreviewView.setLayoutParams(layoutParams);
        layout.addView(cameraPreviewView);
    }

  private void createOverlay() {
        overlayImageView = new ImageView(this);
        String filename = getIntent().getStringExtra(FILENAME) + "_overlay.png";
        Log.d(TAG, " image: " + filename);
        setBitmap(overlayImageView, filename);
        overlayImageView.setScaleType(ScaleType.FIT_XY);
        overlayImageView.setAdjustViewBounds(false);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.topMargin = dpToPixels(48);
        layoutParams.bottomMargin = dpToPixels(100);

        overlayImageView.setLayoutParams(layoutParams);
        
        Log.d(TAG, " max Height" + Integer.toString(overlayImageView.getHeight()));

        layout.addView(overlayImageView);
    }

    private void createCaptureButton() {
        captureButton = new ImageButton(getApplicationContext());
        setBitmap(captureButton, "capture_button.png");
        captureButton.setBackgroundColor(Color.TRANSPARENT);
        captureButton.setScaleType(ScaleType.FIT_CENTER);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(dpToPixels(75), dpToPixels(75));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams.bottomMargin = dpToPixels(10);
        captureButton.setLayoutParams(layoutParams);
        captureButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                setCaptureButtonImageForEvent(event);
                return false;
            }
        });
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePictureWithAutoFocus();
            }
        });
        layout.addView(captureButton);
    }

    private void setCaptureButtonImageForEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setBitmap(captureButton, "capture_button_pressed.png");
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            setBitmap(captureButton, "capture_button.png");
        }
    }

    private void takePictureWithAutoFocus() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            camera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    takePicture();
                }
            });
        } else {
            takePicture();
        }
    }

    private void takePicture() {
        try {
            camera.takePicture(null, null, new PictureCallback() {
                @Override
                public void onPictureTaken(byte[] jpegData, Camera camera) {
                    new OutputCapturedImageTask().execute(jpegData);
                }
            });
        } catch (Exception e) {
            finishWithError("Failed to take image");
        }
    }

    private class OutputCapturedImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... jpegData) {
            try {

                String geartype = getIntent().getStringExtra(FILENAME) ;
                String filename = String.format("%s_%s.%s",geartype, java.util.UUID.randomUUID(), "jpg");
                int quality = getIntent().getIntExtra(QUALITY, 80);
                File capturedImageFile = new File(getCacheDir(), filename);
                Bitmap capturedImage = getScaledBitmap(jpegData[0]);
                capturedImage = correctCaptureImageOrientation(capturedImage);
                FileOutputStream fOut = new FileOutputStream(capturedImageFile);
                capturedImage.compress(CompressFormat.JPEG, quality, fOut);
                fOut.flush();
                fOut.close();
                Intent data = new Intent();
                data.putExtra(IMAGE_URI, Uri.fromFile(capturedImageFile).toString());
                setResult(RESULT_OK, data);
                finish();
            } catch (Exception e) {
                finishWithError("Failed to save image");
            }
            return null;
        }

    }

    private Bitmap getScaledBitmap(byte[] jpegData) {
        int targetWidth = getIntent().getIntExtra(TARGET_WIDTH, -1);
        int targetHeight = getIntent().getIntExtra(TARGET_HEIGHT, -1);
        if (targetWidth <= 0 && targetHeight <= 0) {
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        }

        // get dimensions of image without scaling
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);

        // decode image as close to requested scale as possible
        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);

        // set missing width/height based on aspect ratio
        float aspectRatio = ((float)options.outHeight) / options.outWidth;
        if (targetWidth > 0 && targetHeight <= 0) {
            targetHeight = Math.round(targetWidth * aspectRatio);
        } else if (targetWidth <= 0 && targetHeight > 0) {
            targetWidth = Math.round(targetHeight / aspectRatio);
        }

        // make sure we also
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int requestedWidth, int requestedHeight) {
        int originalHeight = options.outHeight;
        int originalWidth = options.outWidth;
        int inSampleSize = 1;
        if (originalHeight > requestedHeight || originalWidth > requestedWidth) {
            int halfHeight = originalHeight / 2;
            int halfWidth = originalWidth / 2;
            while ((halfHeight / inSampleSize) > requestedHeight && (halfWidth / inSampleSize) > requestedWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap correctCaptureImageOrientation(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void finishWithError(String message) {
        Intent data = new Intent().putExtra(ERROR_MESSAGE, message);
        setResult(RESULT_ERROR, data);
        finish();
    }

    private int dpToPixels(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void setBitmap(ImageView imageView, String imageName) {
        try {
            InputStream imageStream = getAssets().open("www/resources/images/cameraoverlay/" + imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            imageView.setImageBitmap(bitmap);
            imageStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Could load image", e);
        }
    }

}

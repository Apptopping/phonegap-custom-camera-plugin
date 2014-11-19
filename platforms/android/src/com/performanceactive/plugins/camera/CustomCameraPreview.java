/**
 *
 */
package com.performanceactive.plugins.camera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class CustomCameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CustomCameraPreview.class.getSimpleName();

    private final Camera camera;
    private SurfaceHolder mSurfaceHolder;

    public CustomCameraPreview(Context context, Camera camera) {
        super(context);
        this.camera = camera;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // TODO: show activity indicator here, it can take almost 1 second to show the preview
            
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // nothing to do here
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (getHolder().getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
        } catch (Exception e){
            // tried to stop a non-existent preview
        }

        try {
            Camera.Parameters cameraSettings = camera.getParameters();
            List<Size> supportedPreviewSizes = cameraSettings.getSupportedPreviewSizes();
            Size previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
            cameraSettings.setPreviewSize(previewSize.width, previewSize.height);
            camera.setParameters(cameraSettings);
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            Log.d(TAG, " preview Height" + Integer.toString(previewSize.height));

            if (previewSize != null) {
                camera.startPreview();
            }
          
        
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    static Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        final double MAX_DOWNSIZE = 1.5;

        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            double downsize = (double) size.width / w;
            if (downsize > MAX_DOWNSIZE) {
                //if the preview is a lot larger than our display surface ignore it
                //reason - on some phones there is not enough heap available to show the larger preview sizes 
                continue;
            }
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        //keep the max_downsize requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                double downsize = (double) size.width / w;
                if (downsize > MAX_DOWNSIZE) {
                    continue;
                }
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        //everything else failed, just take the closest match
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }

}

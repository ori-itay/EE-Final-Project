package com.android.visualcrypto;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;

import com.android.visualcrypto.flow.Flow;
import com.android.visualcrypto.openCvUtils.OpenCvUtils;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;


public class VideoProcessing extends AppCompatActivity {

    TextureView processedImgTextureView;
    ImageView processedImgView;

    ImageAnalysis imageAnalysis;
    Preview preview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_processing);

        processedImgTextureView = findViewById(R.id.processedImgTextureView);
        processedImgView = findViewById(R.id.processedImgView);

        startCamera();
    }

    private void startCamera() {
        CameraX.unbindAll();

        preview = setPreview();
        imageAnalysis = setImageAnalysis();

        CameraX.bindToLifecycle(this, preview, imageAnalysis);
    }

    private ImageAnalysis setImageAnalysis() {
        HandlerThread analyzerThread = new HandlerThread("VideoProcessing");
        analyzerThread.start();

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE) // need to check for ACQUIRE_NEXT_IMAGE
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                //.setTargetRotation(Surface.ROTATION_0) // has no effect :(
                .setImageQueueDepth(2).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {
            //updateTransform(rotationDegrees);
            //Log.d("rotation", String.valueOf(rotationDegrees) + ": " + String.valueOf((int) processedImgTextureView.getRotation()));
            final Bitmap bp = processedImgTextureView.getBitmap();
            if (bp == null) {
                return;
            }
            Mat mat = new Mat();
            Utils.bitmapToMat(bp, mat);

            /*********WITH CALIBRATION**************/
            Mat afterCalibrationMatrix = OpenCvUtils.calibrateImage(mat);
            Utils.matToBitmap(afterCalibrationMatrix, bp); // update bitmap as well
            /***************************************/

            /*********NO CALIBRATION**************/
            //Mat afterCalibrationMatrix = mat;
            /***************************************/

            try {
                final Bitmap finalBitmap = Flow.executeAndroidFlow(afterCalibrationMatrix, bp, this);
                runOnUiThread(() -> processedImgView.setImageBitmap(finalBitmap));
            } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                    InvalidAlgorithmParameterException | InvalidKeyException | IOException e) {
                e.printStackTrace();
            } catch (Exception rest) {
                Log.d("VideoProcessing", "General exception occured!");
                rest.printStackTrace();
            }
        });

        return imageAnalysis;
    }


    private Preview setPreview() {
        Rational aspectRatio = new Rational(processedImgTextureView.getWidth(), processedImgTextureView.getHeight());
        Size screen = new Size(processedImgTextureView.getWidth(), processedImgTextureView.getHeight());

        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                output -> {
                    ViewGroup parent = (ViewGroup) processedImgTextureView.getParent();
                    parent.removeView(processedImgTextureView);
                    parent.addView(processedImgTextureView, 0);

                    processedImgTextureView.setSurfaceTexture(output.getSurfaceTexture());
                    updateTransform(0);
                });

        return preview;
    }

    private void updateTransform(int rotation) {
        Matrix mx = new Matrix();
        float w = processedImgTextureView.getMeasuredWidth();
        float h = processedImgTextureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        //int rotation = (int) processedImgTextureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        processedImgTextureView.setTransform(mx);
    }
}

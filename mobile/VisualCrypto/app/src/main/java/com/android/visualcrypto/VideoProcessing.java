package com.android.visualcrypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.android.visualcrypto.videoProcessingUtils.TakePictureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class VideoProcessing extends AppCompatActivity {

    ImageView processedImgView;

    Preview preview;
    ImageCapture imageCapture;

    PreviewView previewView;
    ToggleButton toggleButton;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.video_processing);

        processedImgView = findViewById(R.id.processedImgView);
        processedImgView.bringToFront();

        previewView = findViewById(R.id.previewView);
        toggleButton = findViewById(R.id.toggleButton);


        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreviewAndCapture(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public static ExecutorService executor;

    @SuppressLint("ClickableViewAccessibility")
    private void bindPreviewAndCapture(ProcessCameraProvider cameraProvider) {
        preview = setPreview();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = setImageCapture();
        TakePictureCallback.setImageCaptureParams(imageCapture, processedImgView, this);

//TODO : 1) fix preview.
        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                executor = Executors.newFixedThreadPool(2);
                imageCapture.takePicture(executor, new TakePictureCallback());
            } else {
                executor.shutdown();
            }
        });





        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
    }

    private ImageCapture setImageCapture() {
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)//////
                .setTargetRotation(Surface.ROTATION_0)
                //.setTargetRotation(view.getDisplay().getRotation())
                .build();

        return imageCapture;
    }


    private Preview setPreview() {
        Preview preview = new Preview.Builder()./*setTargetResolution(screen).*/build(); // AspectRatio.4_3, .setTargetAspectRatio(aspectRatio)
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        return preview;
    }
}

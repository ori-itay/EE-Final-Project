package com.android.visualcrypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.widget.ImageView;
import android.widget.TextView;
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class handles capturing images in video mode. The Actual process of the images are done in
 * @TakePictureCallback
 */
public class VideoProcessing extends AppCompatActivity {

    ImageView processedImgView;
    Preview preview;
    ImageCapture imageCapture;
    PreviewView previewView;
    ToggleButton toggleButton;
    TextView errorMsgTextView;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Context context;

    public static ExecutorService executor = null;
    public static final BlockingQueue<Bitmap> finishedQueue = new ArrayBlockingQueue<>(8, true);
    public static final Integer THREADPOOL_NUM_THREADS = 3;

    /**
     * Get pointers to the objects, assign listener to the camera provider future
     * @param savedInstanceState - The Bundle state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.video_processing);

        processedImgView = findViewById(R.id.processedImgView);
        processedImgView.bringToFront();

        previewView = findViewById(R.id.previewView);
        toggleButton = findViewById(R.id.toggleButton);

        errorMsgTextView = this.findViewById(R.id.errorMsgView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            while (true){
                try {
                    if (executor != null) {
                        executor.shutdown();
                    }
                    if (toggleButton.isChecked()){
                        toggleButton.setChecked(false);
                    }

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreviewAndCapture(cameraProvider);
                    break;
                } catch (ExecutionException | InterruptedException e) {
                    Log.d("cameraXBind", "Error at starting cameraX API");
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }

        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Bind Preview and ImageCapture use-case to CameraX API
     * @param cameraProvider - The camera provider
     */
    @SuppressLint("ClickableViewAccessibility")
    private void bindPreviewAndCapture(ProcessCameraProvider cameraProvider) {
        preview = setPreview();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = setImageCapture();

        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                executor = Executors.newFixedThreadPool(THREADPOOL_NUM_THREADS);
                imageCapture.takePicture(executor, new TakePictureCallback(imageCapture, this, errorMsgTextView));
            } else {
                executor.shutdown();
            }
        });

        Thread queueThread = new Thread(new ConsumerSideQueue(this, processedImgView));
        queueThread.start();
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
    }

    /**
     * Set ImageCapture use-case
     * @return the ImageCapture
     */
    private ImageCapture setImageCapture() {
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(Surface.ROTATION_0)
                .build();

        return imageCapture;
    }

    /**
     * Set the Preview use-case
     * @return the preview
     */
    private Preview setPreview() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        return preview;
    }
}

package com.android.visualcrypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.android.visualcrypto.flow.Flow;
import com.android.visualcrypto.videoProcessingUtils.RepeatListener;
import com.android.visualcrypto.videoProcessingUtils.TakePictureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.NoSuchPaddingException;

import static com.android.visualcrypto.MainActivity.bitmapToFile;


public class VideoProcessing extends AppCompatActivity {

    ImageView processedImgView;

    Preview preview;
    ImageCapture imageCapture;

    PreviewView previewView;
    ImageButton imageButton;

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
        imageButton = findViewById(R.id.imageButtonUseCase);


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

    private static ExecutorService executor = Executors.newFixedThreadPool(2);

    @SuppressLint("ClickableViewAccessibility")
    private void bindPreviewAndCapture(ProcessCameraProvider cameraProvider) {
        preview = setPreview();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = setImageCapture();
        TakePictureCallback.setImageCaptureParams(executor, imageCapture, processedImgView, this);

        //imageButton.setOnTouchListener(new RepeatListener(1000, 2000, (View.OnClickListener) v -> {
        imageButton.setOnClickListener(v -> //TODO : 1) fix preview. 2) fix onclick, switch to disable etc
            imageCapture.takePicture(executor, new TakePictureCallback()));



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

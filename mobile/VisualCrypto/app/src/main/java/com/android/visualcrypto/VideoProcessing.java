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
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
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

    @SuppressLint("ClickableViewAccessibility")
    private void bindPreviewAndCapture(ProcessCameraProvider cameraProvider) {
        preview = setPreview();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = setImageCapture();

        imageButton.setOnTouchListener(new RepeatListener(1000, 2000, (View.OnClickListener) v -> {
            imageCapture.takePicture(Executors.newSingleThreadExecutor(), new ImageCapture.OnImageCapturedCallback() {

                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    Log.d("threadName", "onCaptureSuccess: " + Thread.currentThread().getName());

                    long endToEndStart = System.nanoTime();
                    long start = System.nanoTime();

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Log.d("time", "mili, buffer to bitmap: " + (System.nanoTime() - start) / 1e6);
                    image.close();
//                    Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
//                    mat.put(0,0,bytes);
//                    Bitmap bp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(mat, bp); // update bitmap as well
                    start = System.nanoTime();
                    Bitmap bp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    Log.d("time", "mili, decodeByteArray: " + (System.nanoTime() - start) / 1e6);


                    /** ROTATION**/
                    //start = System.nanoTime();
                    //Bitmap fixedBitmap = CameraRotationFix.rotateImage(rotatedBitmap, image.getImageInfo().getRotationDegrees());
                    //Log.d("time", "mili, rotateImage: " + (System.nanoTime() - start) / 1e6);
                    /*************/


                    start = System.nanoTime();
                    Mat mat = new Mat();
                    Utils.bitmapToMat(bp, mat);

//                    try {
//                        bitmapToFile(bp);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    /*********WITH CALIBRATION**************/
//                    Mat afterCalibrationMatrix = OpenCvUtils.calibrateImage(mat, false);
//                    Utils.matToBitmap(afterCalibrationMatrix, bp); // update bitmap as well
//                    Log.d("time", "mili, bitmapToMat + calibration: " + (System.nanoTime() - start) / 1e6);
                    /***************************************/

                    /*********NO CALIBRATION**************/
                    Mat afterCalibrationMatrix = mat;
                    /***************************************/

                    try {
                        bitmapToFile(bp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        start = System.nanoTime();
                        final Bitmap finalBitmap = Flow.executeAndroidFlow(afterCalibrationMatrix, bp, context);
                        Log.d("time", "mili, executeAndroidFlow took: " + (System.nanoTime() - start) / 1e6);
                        if (finalBitmap == null) {
                            Log.d("finalBitmap", "finalBitmap is null");
                            return;
                        }


                        runOnUiThread(() -> {
                            long s = System.nanoTime();
                            processedImgView.setImageBitmap(finalBitmap);
                            Log.d("time", "mili,setFinalBitmap took: " + (System.nanoTime() - s) / 1e6);
                            Log.d("time", "mili, ~~~~~EndToEnd~~~~~ took: " + (System.nanoTime() - endToEndStart) / 1e6);
                        });

                    } catch (IOException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchPaddingException e) {
                        e.printStackTrace();
                    } catch (Exception rest) {
                        Log.d("onCaptureSuccess", "General exception occured!");
                        rest.printStackTrace();
                    }
                }



                @Override
                public void onError(@NonNull final ImageCaptureException exception) {
                    exception.printStackTrace();
                    Log.d("imageButton", "ImageCapture UseCase onError!");
                }
            });
        }));


        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
    }

    private ImageCapture setImageCapture() {
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)//////
                .setTargetRotation(Surface.ROTATION_0)
                //.setTargetRotation(view.getDisplay().getRotation())
                .build();
        ;

        return imageCapture;
    }


    private Preview setPreview() {
        Preview preview = new Preview.Builder()./*setTargetResolution(screen).*/build(); // AspectRatio.4_3, .setTargetAspectRatio(aspectRatio)
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        return preview;
    }

//    private void updateTransform(int rotation) {
//        Matrix mx = new Matrix();
//        float w = processedImgTextureView.getMeasuredWidth();
//        float h = processedImgTextureView.getMeasuredHeight();
//
//        float cX = w / 2f;
//        float cY = h / 2f;
//
//        int rotationDgr;
//        //int rotation = (int) processedImgTextureView.getRotation();
//
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                rotationDgr = 0;
//                break;
//            case Surface.ROTATION_90:
//                rotationDgr = 90;
//                break;
//            case Surface.ROTATION_180:
//                rotationDgr = 180;
//                break;
//            case Surface.ROTATION_270:
//                rotationDgr = 270;
//                break;
//            default:
//                return;
//        }
//
//        mx.postRotate((float) rotationDgr, cX, cY);
//        processedImgTextureView.setTransform(mx);
//    }





















}




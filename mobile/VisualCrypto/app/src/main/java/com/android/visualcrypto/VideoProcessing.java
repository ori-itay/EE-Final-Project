package com.android.visualcrypto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.android.visualcrypto.flow.Flow;
import com.android.visualcrypto.openCvUtils.OpenCvUtils;
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

    TextureView processedImgTextureView;
    ImageView processedImgView;

    ImageAnalysis imageAnalysis;
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

        //processedImgTextureView = findViewById(R.id.processedImgTextureView);
        processedImgView = findViewById(R.id.processedImgView);

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
        
        
        //startCamera();
    }

    private void bindPreviewAndCapture(ProcessCameraProvider cameraProvider) {
        preview = setPreview();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageCapture imageCapture = setImageCapture();


        //imageButton.setOnClickListener(v -> {
        imageButton.setOnLongClickListener(v->{
            imageCapture.takePicture(Executors.newCachedThreadPool(), new ImageCapture.OnImageCapturedCallback() { // TODO: FixedThreadPool?
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    long start = System.nanoTime();

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Log.d("time", "mili, buffer to bitmap: " + (System.nanoTime() - start)/1e6);
                    image.close();
                    start = System.nanoTime();
                    Bitmap bp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    Log.d("time", "mili, decodeByteArray: " + (System.nanoTime() - start)/1e6);
                    start = System.nanoTime();
                    //Bitmap fixedBitmap = CameraRotationFix.rotateImage(rotatedBitmap, image.getImageInfo().getRotationDegrees());
                    Log.d("time", "mili, rotateImage: " + (System.nanoTime() - start)/1e6);

//                    runOnUiThread(()-> {
//                        long t = System.nanoTime();
//                        processedImgView.setImageBitmap(rotatedBitmap);
//                        processedImgView.bringToFront();
//                        Log.d("time", "milli setImageBitmap+bringToFront():" + (System.nanoTime() - t)/1e6);
//                    });

                    Mat mat = new Mat();
                    Utils.bitmapToMat(bp, mat);
                    try {
                        bitmapToFile(bp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    /*********WITH CALIBRATION**************/
                    Mat afterCalibrationMatrix = OpenCvUtils.calibrateImage(mat, false);
                    Utils.matToBitmap(afterCalibrationMatrix, bp); // update bitmap as well
                    /***************************************/

                    /*********NO CALIBRATION**************/
//                    Mat afterCalibrationMatrix = mat;
                    /***************************************/

                    try {
                        start = System.nanoTime();
                        final Bitmap finalBitmap = Flow.executeAndroidFlow(afterCalibrationMatrix, bp,  context);
                        Log.d("time", "mili, executeAndroidFlow took: " + (System.nanoTime() - start)/1e6);
                        if (finalBitmap == null) {
                            Log.d("finalBitmap", "finalBitmap is null");
                            return;
                        }


                        runOnUiThread(() -> {
                            processedImgView.setImageBitmap(finalBitmap);
                            processedImgView.bringToFront();
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
            return true;
        });

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageCapture, preview);
    }

    private ImageCapture setImageCapture() {
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)////CAPTURE_MODE_MINIMIZE_LATENCY
                .setTargetRotation(Surface.ROTATION_0)
                //.setTargetRotation(view.getDisplay().getRotation())
                .build();
        ;

        return imageCapture;
    }


//    private void startCamera() {
//        CameraX.unbindAll();
//
//        preview = setPreview();
//        //imageAnalysis = setImageAnalysis();
//        imageCapture = setImageCapture();
//
//        CameraX.bindToLifecycle(this, preview/*, imageAnalysis*/);
//    }

//    private ImageCapture setImageCapture() {
//        ImageCapture imageCapture =
//                new ImageCapture.Builder()
//                        .setTargetRotation(view.getDisplay().getRotation())
//                        .build();
//        return
//    }

//    private ImageAnalysis setImageAnalysis() {
//        HandlerThread analyzerThread = new HandlerThread("VideoProcessing");
//        analyzerThread.start();
//
//        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
//                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE) // need to check for ACQUIRE_NEXT_IMAGE
//                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
//                //.setTargetRotation(Surface.ROTATION_0) // has no effect :(
//                .setImageQueueDepth(2).build();
//
//        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
//        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {
//            //updateTransform(rotationDegrees);
//            //Log.d("rotation", String.valueOf(rotationDegrees) + ": " + String.valueOf((int) processedImgTextureView.getRotation()));
//            final Bitmap bp = processedImgTextureView.getBitmap();
//            if (bp == null) {
//                return;
//            }
////            try {// debug: write the captured frame to file
////                bitmapToFile(bp);
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
//
//            Mat mat = new Mat();
//            Utils.bitmapToMat(bp, mat);
//
//            /*********WITH CALIBRATION**************/
////            Mat afterCalibrationMatrix = OpenCvUtils.calibrateImage(mat, true);
////            Utils.matToBitmap(afterCalibrationMatrix, bp); // update bitmap as well
//            /***************************************/
//
//            /*********NO CALIBRATION**************/
//            Mat afterCalibrationMatrix = mat;
//            /***************************************/
//
//            try {
//                final Bitmap finalBitmap = Flow.executeAndroidFlow(afterCalibrationMatrix, bp, this);
//                runOnUiThread(() -> processedImgView.setImageBitmap(finalBitmap));
//            } catch (NoSuchAlgorithmException | NoSuchPaddingException |
//                    InvalidAlgorithmParameterException | InvalidKeyException | IOException e) {
//                e.printStackTrace();
//            } catch (Exception rest) {
//                Log.d("VideoProcessing", "General exception occured!");
//                rest.printStackTrace();
//            }
//        });
//
//        return imageAnalysis;
//    }


    private Preview setPreview() {
//        Rational aspectRatio = new Rational(processedImgTextureView.getWidth(), processedImgTextureView.getHeight());
//        Size screen = new Size(processedImgTextureView.getWidth(), processedImgTextureView.getHeight());
//        Log.d("setPreview", screen.toString());


        Preview preview = new Preview.Builder()./*setTargetResolution(screen).*/build(); // AspectRatio.4_3, .setTargetAspectRatio(aspectRatio)
        preview.setSurfaceProvider(previewView.createSurfaceProvider());

//        preview.setOnPreviewOutputUpdateListener(
//                output -> {
//                    ViewGroup parent = (ViewGroup) processedImgTextureView.getParent();
//                    parent.removeView(processedImgTextureView);
//                    parent.addView(processedImgTextureView, 0);
//
//                    processedImgTextureView.setSurfaceTexture(output.getSurfaceTexture());
//                    updateTransform(0);
//                });

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

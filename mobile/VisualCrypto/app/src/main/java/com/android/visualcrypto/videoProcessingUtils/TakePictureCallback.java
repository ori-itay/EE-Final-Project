package com.android.visualcrypto.videoProcessingUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

import com.android.visualcrypto.VideoProcessing;
import com.android.visualcrypto.flow.Flow;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import static com.android.visualcrypto.MainActivity.lastDetectedRoi;

public class TakePictureCallback extends ImageCapture.OnImageCapturedCallback {

    private static ImageCapture imageCapture;
    private VideoProcessing videoProcessing;

    public TakePictureCallback(ImageCapture imageCapture, VideoProcessing videoProcessing) {
        TakePictureCallback.imageCapture = imageCapture;
        this.videoProcessing = videoProcessing;
    }

    @Override
    public void onCaptureSuccess(@NonNull ImageProxy image) {
        Log.d("threadName", "onCaptureSuccess: " + Thread.currentThread().getName());

        long endToEndStart = System.nanoTime();
        long start = System.nanoTime();

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Log.d("performance", "mili, buffer to bitmap: " + (System.nanoTime() - start) / 1e6);
        image.close();

        imageCapture.takePicture(VideoProcessing.executor, new TakePictureCallback(imageCapture, videoProcessing));



//                    Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
//                    mat.put(0,0,bytes);
//                    Bitmap bp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(mat, bp); // update bitmap as well
        start = System.nanoTime();
        Bitmap bp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        Log.d("performance", "mili, decodeByteArray: " + (System.nanoTime() - start) / 1e6);


        /** ROTATION**/
        //start = System.nanoTime();
        //Bitmap fixedBitmap = CameraRotationFix.rotateImage(rotatedBitmap, image.getImageInfo().getRotationDegrees());
        //Log.d("performance", "mili, rotateImage: " + (System.nanoTime() - start) / 1e6);
        /*************/


        start = System.nanoTime();
        Mat mat = new Mat();
        Utils.bitmapToMat(bp, mat);

//        try {
//            bitmapToFile(bp);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        /*********WITH CALIBRATION**************/
//                    Mat afterCalibrationMatrix = OpenCvUtils.calibrateImage(mat, false);
//                    Utils.matToBitmap(afterCalibrationMatrix, bp); // update bitmap as well
//                    Log.d("performance", "mili, bitmapToMat + calibration: " + (System.nanoTime() - start) / 1e6);
        /***************************************/

        /*********NO CALIBRATION**************/
        Mat afterCalibrationMatrix = null;
        if (lastDetectedRoi != null &&
                mat.rows() >= lastDetectedRoi.height  &&   mat.cols() >= lastDetectedRoi.width) {
            start = System.nanoTime();
            try {
                afterCalibrationMatrix = new Mat(mat, lastDetectedRoi);
            }catch (Exception e) {

            }
            
            bp = Bitmap.createBitmap(afterCalibrationMatrix.cols(), afterCalibrationMatrix.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(afterCalibrationMatrix, bp);
            Log.d("performance", "mili, crops for lastDetectodRoi took: " + (System.nanoTime() - start) / 1e6);
        } else {
            afterCalibrationMatrix = mat;
        }

//        Mat afterCalibrationMatrix = mat;
        /***************************************/

//        try {
//            bitmapToFile(bp);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            start = System.nanoTime();
            final Bitmap finalBitmap = Flow.executeAndroidFlow(afterCalibrationMatrix, bp, videoProcessing.getApplicationContext());
            Log.d("performance", "mili, executeAndroidFlow took: " + (System.nanoTime() - start) / 1e6);
            if (finalBitmap == null) {
                Log.d("finalBitmap", "finalBitmap is null");
                return;
            }

            long s = System.nanoTime();
            if (VideoProcessing.finishedQueue.remainingCapacity() == 0) {
                Log.d("finishedQueue", "finishedQueue was full!");
                VideoProcessing.finishedQueue.take();
            }
            VideoProcessing.finishedQueue.put(finalBitmap);
            Log.d("performance", "mili,insertToQueue took: " + (System.nanoTime() - s) / 1e6);
            Log.d("performance", "mili, ~~~~~EndToEnd~~~~~ took: " + (System.nanoTime() - endToEndStart) / 1e6);

        } catch (IOException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchPaddingException e) {
            e.printStackTrace();
        }  catch (Exception rest) {
            Log.d("onCaptureSuccess", "General exception occured!");
            rest.printStackTrace();
        }
    }



    @Override
    public void onError(@NonNull final ImageCaptureException exception) {
        exception.printStackTrace();
        Log.d("imageButton", "ImageCapture UseCase onError!");
    }

}

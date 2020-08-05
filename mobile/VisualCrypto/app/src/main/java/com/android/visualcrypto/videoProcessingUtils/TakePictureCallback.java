package com.android.visualcrypto.videoProcessingUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;

import com.android.visualcrypto.ConsumerSideQueue;
import com.android.visualcrypto.VideoProcessing;
import com.android.visualcrypto.flow.BitmapWrapper;
import com.android.visualcrypto.flow.Flow;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

public class TakePictureCallback extends ImageCapture.OnImageCapturedCallback {

    private static ImageCapture imageCapture;
    private VideoProcessing videoProcessing;
    private TextView errorMsgView;

    public TakePictureCallback(ImageCapture imageCapture, VideoProcessing videoProcessing, TextView errorMsgView) {
        TakePictureCallback.imageCapture = imageCapture;
        this.videoProcessing = videoProcessing;
        this.errorMsgView = errorMsgView;
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

        imageCapture.takePicture(VideoProcessing.executor, new TakePictureCallback(imageCapture, videoProcessing, errorMsgView));

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

        /*********WITH CALIBRATION**************/
//                    Mat afterCalibrationMatrix = OpenCvUtils.calibrateImage(mat, false);
//                    Utils.matToBitmap(afterCalibrationMatrix, bp); // update bitmap as well
//                    Log.d("performance", "mili, bitmapToMat + calibration: " + (System.nanoTime() - start) / 1e6);
        /***************************************/

        /*********NO CALIBRATION**************/
//        Mat afterCalibrationMatrix = null;
//        if (lastDetectedRoi != null &&
//                mat.rows() >= lastDetectedRoi.height  &&   mat.cols() >= lastDetectedRoi.width) {
//            start = System.nanoTime();
//            try {
//                afterCalibrationMatrix = new Mat(mat, lastDetectedRoi);
//            }catch (Exception e) {
//                e.printStackTrace();
//                return;
//            }
//
//            bp = Bitmap.createBitmap(afterCalibrationMatrix.cols(), afterCalibrationMatrix.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(afterCalibrationMatrix, bp);
//            Log.d("performance", "mili, crops for lastDetectodRoi took: " + (System.nanoTime() - start) / 1e6);
//        } else {
//            afterCalibrationMatrix = mat;
//        }
        Mat afterCalibrationMatrix = mat;
        /***************************************/


        try {
            start = System.nanoTime();
            final BitmapWrapper bitmapWrapper = Flow.executeAndroidFlow(afterCalibrationMatrix, bp, videoProcessing.getApplicationContext());
            assert bitmapWrapper != null;
            Log.d("performance", "mili, executeAndroidFlow took: " + (System.nanoTime() - start) / 1e6);
            if (bitmapWrapper.error()) {
                BitmapWrapper.notifyUser(errorMsgView, bitmapWrapper.getErrorType(), 1300, videoProcessing);
                Log.d("finalBitmap", "finalBitmap is null");
                return;
            }

            long s = System.nanoTime();
            if (VideoProcessing.finishedQueue.remainingCapacity() == 0) {
                Log.d("finishedQueue", "finishedQueue was full!");
                VideoProcessing.finishedQueue.take();
            }
            VideoProcessing.finishedQueue.put(bitmapWrapper.getBitmap());
            Log.d("performance", "mili,insertToQueue took: " + (System.nanoTime() - s) / 1e6);
            int endToEnd = (int) ((System.nanoTime() - endToEndStart) / 1e6);
            Log.d("performance", "mili, ~~~~~EndToEnd~~~~~ took: " + endToEnd);

            synchronized (ConsumerSideQueue.lastTimes) {
                ConsumerSideQueue.lastTimes.add(endToEnd);
            }

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

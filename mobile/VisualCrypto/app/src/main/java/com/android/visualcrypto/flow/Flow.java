package com.android.visualcrypto.flow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.android.visualcrypto.MainActivity;
import com.android.visualcrypto.openCvUtils.DistortedImageSampler;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This is where all the logic takes place - image processing, deshuffling, decrypting and finally decoding
 */
public class Flow {
    /**
     * @param capturedImg - The captured image in OpenCV's Mat object
     * @param encodedBitmap - The captured image in a Bitmap
     * @param context - The context
     * @return an object that holds the ready bitmap if no error occurred, or otherwise the error itself
     */
    public static BitmapWrapper executeAndroidFlow(Mat capturedImg, Bitmap encodedBitmap, Context context) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, InterruptedException {
        int retVal;
        DistortedImageSampler distortedImageSampler = new DistortedImageSampler(capturedImg, encodedBitmap);

        if (MainActivity.DEBUG) {
            String folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + Instant.now().toString();
            Path path = Paths.get(folderPath);
            try {
                Files.createDirectory(path);
                distortedImageSampler.DEBUG_FOLDER = path;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (MainActivity.DEBUG) { // snr
            InputStream encodedStream = context.getAssets().open("2level_50_50.jpg");
            Bitmap origEncodedBitmap = BitmapFactory.decodeStream(encodedStream);
            distortedImageSampler.tempOrigPixelMatrix = MainActivity.get2DPixelArray(origEncodedBitmap);
            distortedImageSampler.errCounterTotal = 0; distortedImageSampler.errCounterRed = 0;
            distortedImageSampler.errCounterGreen = 0; distortedImageSampler.errCounterBlue = 0;
        }

        if ((retVal = distortedImageSampler.initParameters()) != 0) {
            if (retVal == 1) {
                return new BitmapWrapper(null, true, BitmapWrapper.Error.QR_POS_NOT_DETECTED);
            } else if (retVal == 2) {
                return new BitmapWrapper(null, true, BitmapWrapper.Error.INVALID_ROI);
            } else if (retVal == 3) {
                return new BitmapWrapper(null, true, BitmapWrapper.Error.ALIGNMENT_PATTERN_NOT_FOUND);
            }
            return null; // shouldn't get here
        }

        long start = System.currentTimeMillis();
        int[][] pixelArr = MainActivity.get2DPixelArray(encodedBitmap);
        Log.d("performance", "get2DPixelArray took: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        if (!DisplayDecoder.decodePixelMatrix(distortedImageSampler, pixelArr)){ // checks for iv and dimensions validity
            Log.d("decodePixelMatrix", "decodePixelMatrix returned null");
            return new BitmapWrapper(null, true, BitmapWrapper.Error.IV_OR_DIMS_CHECKSUM);
        }
        Log.d("performance", "decodePixelMatrix took: " + (System.currentTimeMillis() - start));

        if (MainActivity.DEBUG) {
            Imgcodecs.imwrite(distortedImageSampler.DEBUG_FOLDER + "/SAMPLED_PLACES.jpg", distortedImageSampler.debugPathtaken);
            //SNR
            int total_num_of_modules = distortedImageSampler.getModulesInDim()*distortedImageSampler.getModulesInDim();
            double SNR = (double)distortedImageSampler.errCounterTotal / total_num_of_modules;
            Log.d("ALL CHANNELS SNR", "SNR is: " + SNR);
            double AVG_SNR = (double)(distortedImageSampler.errCounterRed+distortedImageSampler.errCounterGreen+distortedImageSampler.errCounterBlue) /
                    (Constants.CHANNELS * total_num_of_modules);
            Log.d("ALL CHANNELS (AVG) SNR", "SNR is: " + AVG_SNR);
            double RED_SNR = (double)distortedImageSampler.errCounterRed / total_num_of_modules;
            Log.d("RED CHANNEL SNR", "SNR is: " + RED_SNR);
            double GREEN_SNR = (double)distortedImageSampler.errCounterGreen / total_num_of_modules;
            Log.d("GREEN CHANNEL SNR", "SNR is: " + GREEN_SNR);
            double BLUE_SNR = (double)distortedImageSampler.errCounterBlue / total_num_of_modules;
            Log.d("BLUE CHANNEL SNR", "SNR is: " + BLUE_SNR);
        }
        /* decode */
        byte[] decodedBytes = distortedImageSampler.getDecodedData();

        /* get iv */
        byte[] iv = distortedImageSampler.getIV();
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // get secret key
        /* Using constant secret key! */
        byte[] const_key = new byte[]{100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115};
        SecretKeySpec secretKeySpec = new SecretKeySpec(const_key, Parameters.encryptionAlgorithm);
        /* ************************** */

        start = System.currentTimeMillis();
        // deshuffle
        byte[] deshuffledBytes = Deshuffle.getDeshuffledBytes(decodedBytes, ivSpec);
        Log.d("performance", "getDeshuffledBytes took: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        /* decrypt */
        byte[] imageBytes = Decryptor.decryptImage(deshuffledBytes, secretKeySpec, ivSpec);
        Log.d("performance", "decryptImage took: " + (System.currentTimeMillis() - start));

        int width = distortedImageSampler.getWidth();
        int height = distortedImageSampler.getHeight();

        start = System.currentTimeMillis();
        /* convert to Bitmap */
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        MainActivity.setBitmapPixels(bmp, imageBytes, width, height);
        Log.d("performance", "createBitmap took: " + (System.currentTimeMillis() - start));

        return new BitmapWrapper(bmp, false, null);
    }


}

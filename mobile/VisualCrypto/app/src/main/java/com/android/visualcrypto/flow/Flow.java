package com.android.visualcrypto.flow;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.android.visualcrypto.MainActivity;
import com.android.visualcrypto.configurationFetcher.DimensionsFetcher;
import com.android.visualcrypto.configurationFetcher.IvFetcher;
import com.android.visualcrypto.openCvUtils.DistortedImageSampler;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;

import org.opencv.core.Mat;

import java.io.IOException;
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

public class Flow{
    public static Mat delete;
    public static Path DEBUG_FOLDER;

    public static Bitmap executeAndroidFlow(Mat capturedImg, Bitmap encodedBitmap, Context context) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, InterruptedException {
        //DEBUG
        String folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + Instant.now().toString();
        Path path = Paths.get(folderPath);
        try {
            Files.createDirectory(path);
            DEBUG_FOLDER = path;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //
        DistortedImageSampler distortedImageSampler = new DistortedImageSampler(capturedImg, encodedBitmap);

//        //delete from here snr
//        InputStream encodedStream = context.getAssets().open("50_50_4Level_colorPos.jpg");
//        Bitmap origEncodedBitmap = BitmapFactory.decodeStream(encodedStream);
//        distortedImageSampler.tempOrigPixelMatrix = MainActivity.get2DPixelArray(origEncodedBitmap);
//        distortedImageSampler.errCounterTotal = 0; distortedImageSampler.errCounterRed = 0;
//        distortedImageSampler.errCounterGreen = 0; distortedImageSampler.errCounterBlue = 0;
//        //to here

        if (distortedImageSampler.initParameters() != 0) {
            return null;
        }

        long start = System.currentTimeMillis();
        int[][] pixelArr = MainActivity.get2DPixelArray(encodedBitmap);
        Log.d("performance", "get2DPixelArray took: " + (System.currentTimeMillis() - start));


        start = System.currentTimeMillis();
        DisplayDecoder.decodePixelMatrix(distortedImageSampler, pixelArr);
        Log.d("performance", "decodePixelMatrix took: " + (System.currentTimeMillis() - start));

        //Imgcodecs.imwrite(Flow.DEBUG_FOLDER + "/SAMPLED_PLACES.jpg", Flow.delete); //TODO: del

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
        /* decode */
        byte[] decodedBytes = distortedImageSampler.getDecodedData();

        /* get iv */
        byte[] iv = IvFetcher.getIV(distortedImageSampler);
        if (iv == null) {
            Log.d("iv", "Cannot decode the image: IV checksums are wrong!");
            //showAlert(context, "Cannot decode the image: IV checksums are wrong!");
            return null;
        }
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
        /* fetch the image dimensions */
        DimensionsFetcher dimensionsFetcher = new DimensionsFetcher(distortedImageSampler);
        int width = dimensionsFetcher.getWidth();
        int height = dimensionsFetcher.getHeight();

        if (width == 0 || height == 0) {
            Log.d("dimensions", "Cannot decode the image: Dimensions checksum are wrong!");
            //showAlert(context, "Cannot decode the image: Dimensions checksum are wrong!");
            return null;
        } else if (width > Constants.MAX_IMAGE_DIMENSION_SIZE || height > Constants.MAX_IMAGE_DIMENSION_SIZE) {
            Log.d("dimensions", "Error: image dimension larger than " + Constants.MAX_IMAGE_DIMENSION_SIZE);
            //showAlert(context, "Error: image dimension larger than " + Constants.MAX_IMAGE_DIMENSION_SIZE);
            return null;
        }

        start = System.currentTimeMillis();
        /* convert to Bitmap */
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        MainActivity.setBitmapPixels(bmp, imageBytes, width, height);
        //MainActivity.setBitmapPixels(bmp, decodedBytes, width, height);
        Log.d("performance", "createBitmap took: " + (System.currentTimeMillis() - start));

        return bmp;
    }
}

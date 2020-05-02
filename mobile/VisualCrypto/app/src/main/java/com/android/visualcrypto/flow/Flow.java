package com.android.visualcrypto.flow;

import android.content.Context;
import android.graphics.Bitmap;
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Flow{

    public static Bitmap executeAndroidFlow(Mat capturedImg, Bitmap encodedBitmap, Context context) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        DistortedImageSampler distortedImageSampler = new DistortedImageSampler(capturedImg, encodedBitmap, context);

        if (distortedImageSampler.initParameters() != 0) {
            return null;
        }

        int[][] pixelArr;

        long start = System.currentTimeMillis();
        pixelArr = MainActivity.get2DPixelArray(encodedBitmap);
        Log.d("performance", "get2DPixelArray took: " + (System.currentTimeMillis() - start));

        /*
        //delete from here
        InputStream encodedStream = context.getAssets().open("50_50_2Level_10pixInModule.jpg");
        Bitmap origEncodedBitmap = BitmapFactory.decodeStream(encodedStream);
        distortedImageSampler.tempOrigPixelMatrix = MainActivity.get2DPixelArray(origEncodedBitmap);
        //to here
         */

        start = System.currentTimeMillis();
        DisplayDecoder.decodePixelMatrix(distortedImageSampler, pixelArr);
        Log.d("performance", "decodePixelMatrix took: " + (System.currentTimeMillis() - start));
        /* decode */
        byte[] decodedBytes = distortedImageSampler.getDecodedData();

        /* get iv */
        byte[] iv = IvFetcher.getIV(distortedImageSampler);
        if (iv == null) {
            ((MainActivity) context).showAlert("Cannot decode the image: IV checksums are wrong!");
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
        Log.d("performance", "decryptImage took: " + (System.currentTimeMillis() - start));

        /* fetch the image dimensions */
        DimensionsFetcher dimensionsFetcher = new DimensionsFetcher(imageBytes);
        int width = dimensionsFetcher.getWidth();
        int height = dimensionsFetcher.getHeight();

        if (width == 0 || height == 0) {
            ((MainActivity) context).showAlert("Cannot decode the image: Dimensions checksum are wrong!");
            return null;
        } else if (width > Constants.MAX_IMAGE_DIMENSION_SIZE || height > Constants.MAX_IMAGE_DIMENSION_SIZE) {
            ((MainActivity) context).showAlert("Error: image dimension larger than " + Constants.MAX_IMAGE_DIMENSION_SIZE);
            return null;
        }

        start = System.currentTimeMillis();
        /* convert to Bitmap */
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        MainActivity.setBitmapPixels(bmp, imageBytes, width, height);
        Log.d("performance", "createBitmap took: " + (System.currentTimeMillis() - start));

        return bmp;
    }
}

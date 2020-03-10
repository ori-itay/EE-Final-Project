package com.android.visualcrypto.flow;

import android.graphics.Bitmap;

import com.android.visualcrypto.MainActivity;
import com.android.visualcrypto.configurationFetcher.DimensionsFetcher;
import com.android.visualcrypto.configurationFetcher.IvFetcher;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;

import java.io.File;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Flow {

    public static Bitmap executeAndroidFlow(Bitmap encodedBitmap) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        RotatedImageSampler rotatedImageSampler;
        int[][] pixelArr;

        pixelArr = MainActivity.get2DPixelArray(encodedBitmap);
        rotatedImageSampler = DisplayDecoder.decodePixelMatrix(pixelArr);
        /* decode */
        byte[] decodedBytes = rotatedImageSampler.getDecodedData();

        /* get iv */
        byte[] iv = IvFetcher.getIV(rotatedImageSampler);
        if (iv == null) {
            //showAlert("Cannot decode the image: IV checksums are wrong!");
            return null;
        }
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        // get secret key
        /* Using constant secret key! */
        byte[] const_key = new byte[]{100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115};
        SecretKeySpec secretKeySpec = new SecretKeySpec(const_key, Parameters.encryptionAlgorithm);
        /* ************************** */

        // deshuffle
        byte[] deshuffledBytes = Deshuffle.getDeshuffledBytes(decodedBytes, ivSpec);

        /* decrypt */
        byte[] imageBytes = Decryptor.decryptImage(deshuffledBytes, secretKeySpec, ivSpec);

        /* fetch the image dimensions */
        DimensionsFetcher dimensionsFetcher = new DimensionsFetcher(imageBytes);
        int width = dimensionsFetcher.getWidth();
        int height = dimensionsFetcher.getHeight();

        if (width == 0 || height == 0) {
            //showAlert("Cannot decode the image: Dimensions checksum are wrong!");
            return null;
        } else if (width > Constants.MAX_IMAGE_DIMENSION_SIZE || height > Constants.MAX_IMAGE_DIMENSION_SIZE) {
            //showAlert("Error: image dimension larger than " + Constants.MAX_IMAGE_DIMENSION_SIZE);
            return null;
        }

        /* convert to Bitmap */
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        MainActivity.setBitmapPixels(bmp, imageBytes, width, height);

        return bmp;
    }
}

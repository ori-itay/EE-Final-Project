package com.android.visualcrypto.cameraUtils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.util.TimingLogger;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;

public class CameraRotationFix {

    public static Bitmap fixRotation(Bitmap bitmap, String photoPath) throws IOException {
        long tStart = System.currentTimeMillis();
        ExifInterface ei = new ExifInterface(photoPath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap;
        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                rotatedBitmap = rotateImage(bitmap, 90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                rotatedBitmap = rotateImage(bitmap, 180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                rotatedBitmap = rotateImage(bitmap, 270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                rotatedBitmap = bitmap;
        }
        long delta = System.currentTimeMillis() - tStart;
        Log.d("fixRotation", delta / 1000.0 + "seconds");
        return rotatedBitmap;
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }
}
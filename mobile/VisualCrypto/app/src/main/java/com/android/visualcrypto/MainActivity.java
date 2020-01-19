package com.android.visualcrypto;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.visualcrypto.configurationFetcher.DimensionsFetcher;
import com.android.visualcrypto.configurationFetcher.IvFetcher;
import com.pc.configuration.Constants;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;

import java.io.File;
import java.nio.ByteBuffer;
import java.lang.Short;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void decodeImage(View view) {
        getPermissions();

        try {
            File imgFile = null;
            RotatedImageSampler rotatedImageSampler = null;
            int[][] pixelArr;
            /* Display decoded image */
            if (view.getId() == R.id.decodeImgBtn) {
                imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "encodedImage.png");
                if (!imgFile.exists()) {
                    showAlert("Couldn't find 'encodedImage.png' in Downloads");
                    return;
                }

                pixelArr = get2DPixelArray(imgFile);
                rotatedImageSampler = DisplayDecoder.decodePixelMatrix(pixelArr);
                /* decode */
                byte[] decodedBytes = rotatedImageSampler.getDecodedData();

                /* get iv */
                byte[] iv = IvFetcher.getIV(rotatedImageSampler);
                if (iv == null) {
                    showAlert("Cannot decode the image: IV checksums are wrong!");
                    return;
                }
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                // get secret key
                /* Using constant secret key! */
                byte[] const_key = new byte[] {100, 101, 102, 103, 104, 105, 106 ,107, 108, 109, 110, 111, 112, 113, 114, 115};
                SecretKeySpec secretKeySpec = new SecretKeySpec(const_key, Constants.ENCRYPTION_ALGORITHM);
                /* ************************** */

                // deshuffle
                byte[] deshuffledBytes = Deshuffle.getDeshuffledBytes(decodedBytes, ivSpec);

                /* decrypt */
                byte[] imageBytes = Decryptor.decryptImage(deshuffledBytes, secretKeySpec, ivSpec);

                /* fetch the image dimensions */
                DimensionsFetcher dimensionsFetcher = new DimensionsFetcher(imageBytes);
                int width = dimensionsFetcher.getWidth();
                int height = dimensionsFetcher.getHeight();

                if (width == 0 || height == 0){
                    showAlert("Cannot decode the image: Dimensions checksum are wrong!");
                    return;
                } else if (width > Constants.MAX_IMAGE_DIMENSION_SIZE || height > Constants.MAX_IMAGE_DIMENSION_SIZE){
                    showAlert("Error: image dimension larger than " + Constants.MAX_IMAGE_DIMENSION_SIZE);
                    return;
                }

                /* convert to Bitmap */
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                setBitmapPixels(bmp, imageBytes, width, height);

                /* display the image */
                ImageView iView = (ImageView) findViewById(R.id.decodedImgId);
                Log.d("iviewparameters", String.format("width: %d, height: %d", iView.getWidth(), iView.getHeight()));
                iView.setImageBitmap(Bitmap.createScaledBitmap(bmp, iView.getWidth(), iView.getHeight(), false));
            } /* Display decoded text */
            /*else if (view.getId() == R.id.decodeTxtBtn) {
                imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "out.png");
                if (!imgFile.exists()) {
                    showAlert("Couldn't find 'out.png' in Downloads");
                    return;
                }
                pixelArr = get2DPixelArray(imgFile);
                rotatedImageSampler = DisplayDecoder.decodePixelMatrix(pixelArr);

                TextView dataCfgText = (TextView) findViewById(R.id.dataCfg);
                SpannableStringBuilder dataTxt = new SpannableStringBuilder(getString(R.string.dataText));
                dataTxt.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, dataTxt.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                dataTxt.setSpan(new RelativeSizeSpan(2f), 0, dataTxt.length(), 0);
                dataTxt.setSpan(new ForegroundColorSpan(Color.BLACK), 0, dataTxt.length(), 0);
                dataCfgText.setText(dataTxt.append(new String(rotatedImageSampler.getDecodedData())));
            }

            if (imgFile == null){
                return;
            }

            TextView dataLengthCfgText = (TextView) findViewById(R.id.dataLengthCfg);
            TextView moduleSizeCfgText = (TextView) findViewById(R.id.moduleSizeCfg);

            SpannableStringBuilder dataLengthTxt = new SpannableStringBuilder(getString(R.string.dataLengthText));
            dataLengthTxt.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, dataLengthTxt.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dataLengthTxt.setSpan(new RelativeSizeSpan(2f), 0, dataLengthTxt.length(), 0);
            dataLengthTxt.setSpan(new ForegroundColorSpan(Color.BLACK), 0, dataLengthTxt.length(), 0);


            SpannableStringBuilder moduleSizeTxt = new SpannableStringBuilder(getString(R.string.moduleSizeText));
            moduleSizeTxt.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, moduleSizeTxt.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            moduleSizeTxt.setSpan(new RelativeSizeSpan(2f), 0, moduleSizeTxt.length(), 0);
            moduleSizeTxt.setSpan(new ForegroundColorSpan(Color.BLACK), 0, moduleSizeTxt.length(), 0);

            dataLengthCfgText.setText(dataLengthTxt.append(String.valueOf(rotatedImageSampler.getDataLength())));
            moduleSizeCfgText.setText(moduleSizeTxt.append(String.valueOf(rotatedImageSampler.getModuleSize())));*/

        } catch (Exception e) {
            showAlert("Exception occurred: " + e.getMessage());
            Log.d("decodeFile exception:", e.getMessage());
        }


    }

    private void showAlert(String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }


    private int[][] get2DPixelArray(File imgFile) {
        int[][] twoDimPixels;
        Bitmap bMap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        int width = bMap.getWidth(); int height = bMap.getHeight();
        Log.d("parameters: ",  "Width,Height: " + width+ ", " + height);
        int[] pixels = new int[width*height];
        twoDimPixels = new int[width][height];
        bMap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int row = 0 ; row < width; row++){
            for (int col = 0 ; col < height; col++){
                twoDimPixels[row][col] = pixels[row * width + col];
            }
        }
        return twoDimPixels;
    }

    private void setBitmapPixels(Bitmap bmp, byte[] imageBytes, int width, int height){
        final int METADATA_LENGTH = 5;
        int index, ARGB;
        ByteBuffer wrapped;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                index = (row*width + col)*4;
                wrapped = ByteBuffer.wrap(imageBytes, METADATA_LENGTH + index, 4);
                ARGB = wrapped.getInt();
                bmp.setPixel(col, row, ARGB);
            }
        }
    }

    private void getPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            }
        }
    }
}


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

import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.RotatedImageSampler;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

//    public void getImageFromFile(View view){
//        boolean phone = true;
//
//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
//                PackageManager.PERMISSION_GRANTED){
//
//            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//
//            } else {
//                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
//            }
//        }
//
//
//        if (phone){
//            File imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "qrcode.png");
//            if (imgFile.exists()){
//                Bitmap bMap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//                int width = bMap.getWidth();
//                int height = bMap.getHeight();
//                int pixelMax = 0x00FFFFFF;
//                int[] pixels = new int[width*height];
//                bMap.getPixels(pixels, 0, width, 0, 0, width, height);
//
//                for (int i = 0; i < width*height ; i++){
//                    pixels[i] ^= pixelMax;
//                }
//                bMap = bMap.copy(Bitmap.Config.ARGB_8888, true);
//                bMap.setPixels(pixels, 0, width, 0, 0, width, height);
//
//                ImageView iView = (ImageView) findViewById(R.id.imgDisplay);
//                iView.setImageBitmap(bMap);
//            }
//        }
//    }


    public void decodeImage(View view) {
        getPermissions();

        try {
            File imgFile = null;
            RotatedImageSampler rotatedImageSampler = null;
            int[][] pixelArr;
            /* Display decoded image */
            if (view.getId() == R.id.decodeImgBtn) {
                imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "encoded100.png");
                if (!imgFile.exists()) {
                    showAlert("Couldnt find 'encoded100.png' in Downloads");
                    return;
                }

                pixelArr = get2DPixelArray(imgFile);
                rotatedImageSampler = DisplayDecoder.decodePixelMatrix(pixelArr);
                /* get decoded image byte array */
                byte[] imageBytes = rotatedImageSampler.getDecodedData();
                Log.d("imgDecodeInfo", String.format("Total image bits in array: %d | Total bits width: %d | Total bits height: %d",
                        imageBytes.length*8,rotatedImageSampler.getWidth(), rotatedImageSampler.getHeight()));
                /* convert it to Bitmap */
                Bitmap bmp = Bitmap.createBitmap(rotatedImageSampler.getWidth(), rotatedImageSampler.getHeight(), Bitmap.Config.ARGB_8888);
                Buffer buff = ByteBuffer.wrap(imageBytes);
                bmp.copyPixelsFromBuffer(buff);

                ImageView iView = (ImageView) findViewById(R.id.decodedImgId);
                iView.setImageBitmap(Bitmap.createScaledBitmap(bmp, iView.getWidth(), iView.getHeight(), false));
            } /* Display decoded text */
            else if (view.getId() == R.id.decodeTxtBtn) {
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
            moduleSizeCfgText.setText(moduleSizeTxt.append(String.valueOf(rotatedImageSampler.getModuleSize())));

        } catch (Exception e) {
            showAlert("decodeFile threw exception");
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


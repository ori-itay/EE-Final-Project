package com.android.visualcrypto;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.visualcrypto.cameraUtils.CameraRotationFix;
import com.android.visualcrypto.configurationFetcher.DimensionsFetcher;
import com.android.visualcrypto.configurationFetcher.IvFetcher;
//import com.android.visualcrypto.opencvSampler.OpencvSampler;
import com.android.visualcrypto.openCvSampler.OpenCvSampler;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initDebug()) {showAlert("OpenCV failed to load..Exiting"); return;}
        setContentView(R.layout.activity_main);
        getPermissions(); // gets camera and write permissions
        showEncodedImage();
        Button captureImageBTN = (Button) this.findViewById(R.id.captureImageBTN);
        captureImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        Button decodeImageBTN = (Button) this.findViewById(R.id.decodeImgBtn);
        decodeImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decodeImage();
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TEMP_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".png",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                showAlert("Error while creating photoFile");
                Log.d("photoFile","Error while creating photoFile", ex);
                return;
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.android.visualcrypto", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
                File file = new File(currentPhotoPath);
                try {
                    Bitmap bmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));
                    if (bmap != null) {
                        Bitmap rotatedBitmap = CameraRotationFix.fixRotation(bmap, file.getAbsolutePath());
                        ImageView iView = (ImageView) findViewById(R.id.decodedImgId);
                        iView.setImageBitmap(rotatedBitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (file.exists()){
                        file.delete();
                    }
                }
            }
        }
    }

    private void showEncodedImage() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/encodedImage.png";
        //File imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "encodedImage.png");
//        if (!imgFile.exists()) {
//            showAlert("Couldn't find 'encodedImage.png' in Downloads");
//            return;
//        }
        Bitmap bp = BitmapFactory.decodeFile(path);
        ImageView iView = (ImageView) findViewById(R.id.decodedImgId);
        iView.setImageBitmap(bp);

    }

    public void decodeImage() {
        try {
            OpenCvSampler sampler = new OpenCvSampler();
            //InputStream is = this.getAssets().open("encodedImage.png");
            InputStream is = this.getAssets().open("capturedEncoded.jpeg");
            Bitmap b = BitmapFactory.decodeStream(is);
            sampler.locatePositionDetectors(b);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            long startTime = System.nanoTime();

            File imgFile = null;
            RotatedImageSampler rotatedImageSampler = null;
            int[][] pixelArr;
            /* Display decoded image */
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

            Log.d("performance", String.format("took: %s", System.nanoTime() - startTime));

            /* display configuration information */
            showConfigurationInfo(rotatedImageSampler, height, width);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                InvalidKeyException | NoSuchPaddingException e) {
            showAlert("Exception in decodeFile: " + e.getCause());
            Log.e("decodeFile exception:", e.getStackTrace().toString());
        }
    }

    private void showConfigurationInfo(RotatedImageSampler rotatedImageSampler, int height, int width) {
        TextView moduleSizeText = (TextView) findViewById(R.id.moduleSizeCfg);
        moduleSizeText.setText("Pixels in module dimension: " + rotatedImageSampler.getModuleSize());

        TextView imageHeightText = (TextView) findViewById(R.id.imageHeightCfg);
        imageHeightText.setText("Image Height: " + height);

        TextView imageWidthText = (TextView) findViewById(R.id.imageWidthCfg);
        imageWidthText.setText("Image Width: " + width);
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
        final byte ALPHA_VALUE = (byte) 0xff;
        final int METADATA_LENGTH = 5;
        int index, ARGB;
        ByteBuffer wrapped;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                index = (row*width + col)*Constants.CHANNELS;
                if (Constants.CHANNELS == 4) {
                    wrapped = ByteBuffer.wrap(imageBytes, METADATA_LENGTH + index, Constants.CHANNELS);
                } else if (Constants.CHANNELS == 3) {
                    wrapped = ByteBuffer.allocate(4);
                    wrapped.put(ALPHA_VALUE);
                    wrapped.put(imageBytes, METADATA_LENGTH + index, Constants.CHANNELS);
                    wrapped.position(0); // Sets the position to the start of the ByteBuffer
                }

                ARGB = wrapped.getInt();
                bmp.setPixel(col, row, ARGB);
            }
        }
    }

    private void getPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {}
            else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            }
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.CAMERA)) {}
            else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},1);
            }
        }
    }
}

/* Example of setSpan with colors etc
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


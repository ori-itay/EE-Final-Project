package com.android.visualcrypto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.visualcrypto.cameraUtils.CameraRotationFix;
import com.android.visualcrypto.flow.Flow;
import com.android.visualcrypto.openCvUtils.OpenCvUtils;
import com.pc.configuration.Constants;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.NoSuchPaddingException;


public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initDebug()) {
            showAlert("OpenCV failed to load..Exiting");
            return;
        }
        setContentView(R.layout.activity_main);
        getPermissions(); // gets camera and write permissions
        showEncodedImage();
        Button captureImageBTN = this.findViewById(R.id.captureImageBTN);
        captureImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        Button decodeImageBTN = this.findViewById(R.id.decodeImgBtn);
        decodeImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decodeImage();
            }
        });
        //zxing - maybe for next stage
        //IntentIntegrator integrator = new IntentIntegrator(this);
        //integrator.initiateScan();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TEMP_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                showAlert("Error while creating photoFile");
                Log.d("photoFile", "Error while creating photoFile", ex);
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

    Bitmap rotatedBp;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
                File file = new File(currentPhotoPath);
                try {
                    Bitmap bmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));
                    if (bmap != null) {
                        Bitmap rotatedBitmap = CameraRotationFix.fixRotation(bmap, file.getAbsolutePath());
                        this.rotatedBp = rotatedBitmap;
                        ImageView iView = findViewById(R.id.decodedImgId);
                        iView.setImageBitmap(rotatedBitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (file.exists()) {
                        //boolean res = file.delete();
                        //if (!res) {
                        Log.d("Cleaning", "Unable to delete captured photo");
                        // }
                    }
                }
            }
        }
    }

    private void showEncodedImage() {
        InputStream encodedStream;
        try {
            encodedStream = getAssets().open("encodedImage.jpg");
            Bitmap encodedBitmap = BitmapFactory.decodeStream(encodedStream);

            ImageView iView = findViewById(R.id.decodedImgId);
            iView.setImageBitmap(encodedBitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test(View v) throws IOException, CameraAccessException {

        InputStream is = this.getAssets().open("captured50_50_2levels_10pixInModule_alignmentPattern2.jpg");
        Bitmap b = BitmapFactory.decodeStream(is);

        Mat capturedImage = new Mat();
        Utils.bitmapToMat(b, capturedImage);





//        DistortedImageSampler sampler = new DistortedImageSampler(capturedImage, b, this);
//        boolean found = sampler.detect(capturedImage);
//        int[] intArray = new int[b.getWidth()*b.getHeight()];
//        //copy pixel data from the Bitmap into the 'intArray' array
//        b.getPixels(intArray, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());

        //Flow.executeAndroidFlow(capturedImage, )

    }

    private void decodeImage() {
        try {
            long startTime = System.nanoTime();

            InputStream encodedStream = getAssets().open("orig_50_50_4Level_10pixInModule.jpg");
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/orig_50_50_4Level_10pixInModule.jpg");

            Bitmap encodedBitmap = BitmapFactory.decodeStream(encodedStream);
            Bitmap rotatedBitmap = CameraRotationFix.fixRotation(encodedBitmap, file.getAbsolutePath());

            Mat capturedImage = new Mat();
            Utils.bitmapToMat(rotatedBitmap, capturedImage);

            Mat afterCalibrationMatrix = OpenCvUtils.calibrateImage(capturedImage);
            rotatedBitmap = convertMatToBitmap(afterCalibrationMatrix); // update bitmap as well

            Bitmap resBitmap = Flow.executeAndroidFlow(afterCalibrationMatrix, rotatedBitmap, this);

            if (resBitmap == null) {
                return;
            }

            /* display the image */
            ImageView iView = findViewById(R.id.decodedImgId);
            iView.setImageBitmap(Bitmap.createScaledBitmap(resBitmap, iView.getWidth(), iView.getHeight(), false));

            Log.d("performance", String.format("took: %s", System.nanoTime() - startTime));

            /* display configuration information */
            //showConfigurationInfo(rotatedImageSampler, height, width);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IOException e) {
            showAlert("Exception in decodeImage: " + e);
            Log.e("decodeImage", "decodeFile exception", e);
        }
    }



    public static Mat SimplestColorBalance(Mat img, int percent) {
        if (percent <= 0)
            percent = 5;
        img.convertTo(img, CvType.CV_32F);
        List<Mat> channels = new ArrayList<>();
        int rows = img.rows(); // number of rows of image
        int cols = img.cols(); // number of columns of image
        int chnls = img.channels(); //  number of channels of image
        double halfPercent = percent / 200.0;
        if (chnls >= 3) Core.split(img, channels);
        else channels.add(img);
        List<Mat> results = new ArrayList<>();
        for (int i = 0; i < chnls; i++) {
            // find the low and high precentile values (based on the input percentile)
            Mat flat = new Mat();
            channels.get(i).reshape(1, 1).copyTo(flat);
            Core.sort(flat, flat, Core.SORT_ASCENDING);
            double lowVal = flat.get(0, (int) Math.floor(flat.cols() * halfPercent))[0];
            double topVal = flat.get(0, (int) Math.ceil(flat.cols() * (1.0 - halfPercent)))[0];
            // saturate below the low percentile and above the high percentile
            Mat channel = channels.get(i);
            for (int m = 0; m < rows; m++) {
                for (int n = 0; n < cols; n++) {
                    if (channel.get(m, n)[0] < lowVal)
                        channel.put(m, n, lowVal);
                    if (channel.get(m, n)[0] > topVal)
                        channel.put(m, n, topVal);
                }
            }
            Core.normalize(channel, channel, 0.0, 255.0 / 2, Core.NORM_MINMAX);
            channel.convertTo(channel, CvType.CV_32F);
            results.add(channel);
        }
        Mat outval = new Mat();
        Core.merge(results, outval);
        return outval;
    }

    public void showAlert(String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }


    public static int[][] get2DPixelArray(Bitmap bMap) {
        int[][] twoDimPixels;
        int width = bMap.getWidth();
        int height = bMap.getHeight();
        Log.d("parameters: ", "Width,Height: " + width + ", " + height);
        int[] pixels = new int[width * height];
        twoDimPixels = new int[height][width];
        bMap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                twoDimPixels[row][col] = pixels[row * width + col];
            }
        }

        return twoDimPixels;
    }

    public static void bitmapToFile(Bitmap bp) throws IOException {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "output.jpg");
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        bp.compress(Bitmap.CompressFormat.JPEG, 100, os);
        os.close();
    }

    public static void setBitmapPixels(Bitmap bmp, byte[] imageBytes, int width, int height) {
        final byte ALPHA_VALUE = (byte) 0xff;
        final int METADATA_LENGTH = 5;
        int index, ARGB;
        ByteBuffer wrapped;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                index = (row * width + col) * Constants.CHANNELS;
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
                PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }


        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 2);
            }
        }
    }


    public static Bitmap convertMatToBitmap(Mat mat) {
        Bitmap bp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bp);
        return bp;
    }

    private byte[] convertBitmapToByteArray(Bitmap bp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

}


//    private void showConfigurationInfo(RotatedImageSampler rotatedImageSampler, int height, int width) {
//        TextView moduleSizeText = (TextView) findViewById(R.id.moduleSizeCfg);
//        moduleSizeText.setText("Pixels in module dimension: " + rotatedImageSampler.getModuleSize());
//
//        TextView imageHeightText = (TextView) findViewById(R.id.imageHeightCfg);
//        imageHeightText.setText("Image Height: " + height);
//
//        TextView imageWidthText = (TextView) findViewById(R.id.imageWidthCfg);
//        imageWidthText.setText("Image Width: " + width);
//    }


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


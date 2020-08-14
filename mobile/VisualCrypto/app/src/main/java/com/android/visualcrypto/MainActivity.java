package com.android.visualcrypto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.visualcrypto.cameraUtils.CameraRotationFix;
import com.android.visualcrypto.flow.BitmapWrapper;
import com.android.visualcrypto.flow.Flow;
import com.pc.configuration.Constants;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.crypto.NoSuchPaddingException;


/**
 * This class handle the main content screen and the use-case of single capture mode. For video mode see @VideoProcessing
 */
public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static final int VIDEO_PROCESS_INTENT = 5;
    private String currentPhotoPath;
    public static byte[] privateKey;

    public static final boolean DEBUG = false;
    public static boolean DEBUG_READ_IMAGE_FROM_FILE = false;

    public static Rect lastDetectedRoi = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initDebug()) {
            showAlert(this, "OpenCV failed to load..Exiting");
            return;
        }
        getPermissions(); // gets camera and write permissions

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        Map<String, ?> preferencesMap = sharedPref.getAll();
        if (preferencesMap.size() == 1) {
            Map.Entry<String, ?> entry =  preferencesMap.entrySet().iterator().next();
            String emailStr = entry.getKey();
            String key = (String) entry.getValue();
            privateKey = Base64.getDecoder().decode(key);
            setMainContentView(emailStr, sharedPref);
        } else {
            setContentView(R.layout.entry_window);
            setRegisterButton(sharedPref);
            setSignInButton(sharedPref);
        }
    }

    /**
     * Callback of the sign in button
     */
    private void setSignInButton(SharedPreferences sharedPref) {
        Button signInButton = this.findViewById(R.id.signInBTN);
        EditText email = this.findViewById(R.id.signInEmailTXT);
        EditText secretKey = this.findViewById(R.id.signInSecretKeyTXT);

        signInButton.setOnClickListener((v)-> {
            String emailStr = Objects.requireNonNull(email.getText()).toString();
            String secretKeyStr = Objects.requireNonNull(secretKey.getText()).toString();
            if (emailStr.isEmpty() || secretKeyStr.isEmpty() || !emailStr.contains("@")) {
                showAlert(this, "Error: Invalid input!");
            } else {
                privateKey = Base64.getDecoder().decode(secretKeyStr);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(emailStr, secretKeyStr);
                editor.apply();

                setMainContentView(emailStr, sharedPref);
            }
        });

    }

    /**
     * Callback of the register button
     */
    private void setRegisterButton(SharedPreferences sharedPref) {
        Button registerBTN = this.findViewById(R.id.registerBTN);
        EditText email = this.findViewById(R.id.inputEmail);
        EditText serverAddr = this.findViewById(R.id.serverAddr);

        registerBTN.setOnClickListener((v)-> {
            String emailStr = Objects.requireNonNull(email.getText()).toString();
            String serverAddrStr = Objects.requireNonNull(serverAddr.getText()).toString();
            if (emailStr.isEmpty() || serverAddrStr.isEmpty() || !emailStr.contains("@")) {
                showAlert(this, "Error: Invalid input!");
            } else {
                Thread sendKeyRequest = new Thread(()-> requestSecretKey(serverAddrStr, emailStr));
                sendKeyRequest.start();

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Enter your secret key that you received in the email:");


                final EditText input = new EditText(this);

                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);

                builder.setPositiveButton("OK", (dialog, which) -> {
                    String val = input.getText().toString();
                    privateKey = Base64.getDecoder().decode(val);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(emailStr, val);
                    editor.apply();


                    setMainContentView(emailStr, sharedPref);

                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                builder.show();
            }
        });
    }

    /**
     * Contacts VisualCrypto server and requests a secret key exchange
     * @param serverAddr - The server IP address
     * @param emailStr - The user's email address (username)
     */
    private void requestSecretKey(String serverAddr, String emailStr) {
        try (Socket socket = new Socket(InetAddress.getByName(serverAddr), 32326)) {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write("keyrequest:" + emailStr + "\nover");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the main content view and assign listeners
     */
    private void setMainContentView(String emailStr, SharedPreferences sharedPref) {
        setContentView(R.layout.activity_main);
        Button captureImageBTN = this.findViewById(R.id.captureImageBTN);
        captureImageBTN.setOnClickListener(v -> takePicture());
        Button decodeImageBTN = this.findViewById(R.id.decodeImgBtn);
        decodeImageBTN.setOnClickListener(v -> decodeImage());

        Button videoProcessBTN = this.findViewById(R.id.videoProcessBtn);
        videoProcessBTN.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), VideoProcessing.class);
            startActivityForResult(intent, VIDEO_PROCESS_INTENT);
        });

        TextView loggedInAs = this.findViewById(R.id.loggedInAs);
        loggedInAs.setText("Logged in as: " + emailStr);

        TextView deleteAllUsers = this.findViewById(R.id.deleteAllUsersBTN);
        deleteAllUsers.setOnClickListener((v) -> {
            sharedPref.edit().clear().apply();
            setContentView(R.layout.entry_window);
            setRegisterButton(sharedPref);
            setSignInButton(sharedPref);
        });
    }

    /**
     * Utility to create an image file
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TEMP_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Takes single picture (uses Camera 1 API)
     */
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                showAlert(this, "Error while creating photoFile");
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

    /**
     * Handle the camera capture activity
     */
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
                        ImageView iView = findViewById(R.id.decodedImgId);
                        iView.setImageBitmap(rotatedBitmap);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (file.exists()) {
                        boolean res = file.delete();
                        if (!res) {
                            Log.d("Cleaning", "Unable to delete captured photo");
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a encoded image on main screen
     */
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

    /**
     * Decode single-captured photo
     */
    private void decodeImage() {
        try {
            //long startTime = System.currentTimeMillis();
            /*******************DECODE BY FILE NAME*****************************************/
//            String imageName = "6bits_100_100.jpg";
//            InputStream encodedStream = getAssets().open(imageName);
//            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + imageName);
//
//            Bitmap encodedBitmap = BitmapFactory.decodeStream(encodedStream);
//            /*           Bitmap rotatedBitmap = encodedBitmap; //del*/
//            Bitmap rotatedBitmap = CameraRotationFix.fixRotation(encodedBitmap, file.getAbsolutePath());
            /*******************************************************************************/

            /*******************DECODE LAST TAKEN FILE AUTOMATICALLY************************/
            Bitmap encodedBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            Bitmap rotatedBitmap = CameraRotationFix.fixRotation(encodedBitmap, currentPhotoPath);
            /*******************************************************************************/

            Mat capturedImage = new Mat();
            Utils.bitmapToMat(rotatedBitmap, capturedImage);
            BitmapWrapper resBitmapWrapper = Flow.executeAndroidFlow(capturedImage, rotatedBitmap, this);

            assert resBitmapWrapper != null;
            if (resBitmapWrapper.error()) {
                BitmapWrapper.notifyUser(this.findViewById(R.id.errorMsgSingle), resBitmapWrapper.getErrorType(), 2000, null);
                return;
            }

            /* display the image */
            ImageView iView = findViewById(R.id.decodedImgId);
            iView.setImageBitmap(Bitmap.createScaledBitmap(resBitmapWrapper.getBitmap(), iView.getWidth(), iView.getHeight(), false));

            Log.d("performance", String.format("End time: %d", System.currentTimeMillis()));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IOException | InterruptedException e) {
            showAlert(this, "Exception in decodeImage: " + e);
            Log.e("decodeImage", "decodeFile exception", e);
        } catch (Exception rest) {
            Log.d("onCaptureSuccess", "General exception occured!");
            rest.printStackTrace();
        }
    }

    /**
     * Shows an alert to the user
     * @param context - The context
     * @param msg - The message to the user
     */
    private static void showAlert(Context context, String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    /**
     * Get two dimensional int array from a bitmap
     * @param bMap - The bitmap
     * @return the bitmap data in a two dimensional int array
     */
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

    /**
     * Sets data to the bitmap from a byte array
     * @param bmp - The bitmap
     * @param imageBytes - The data
     * @param width - Width of the photo
     * @param height - Height of the photo
     */
    public static void setBitmapPixels(Bitmap bmp, byte[] imageBytes, int width, int height) {
        final byte ALPHA_VALUE = (byte) 0xff;
        final int METADATA_LENGTH = 0;
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

    /**
     * Sends permission requests to the user
     */
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
}


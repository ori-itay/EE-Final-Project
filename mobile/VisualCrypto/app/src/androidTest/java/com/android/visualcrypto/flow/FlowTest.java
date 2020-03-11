package com.android.visualcrypto.flow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.android.visualcrypto.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import static org.junit.Assert.assertEquals;

// Runs on Android
@RunWith(AndroidJUnit4ClassRunner.class)
public class FlowTest {

    private final String ENCODED_IMG_PATH = "encodedTest1.jpg";
    private final String ORIGINAL_IMG_PATH = "originalTest1.jpg";

    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testAndroidFlow() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        //MainActivity activity = rule.getActivity();

        //View viewById = activity.findViewById(R.id.decodedImgId);

        try {
            InputStream encodedStream = appContext.getAssets().open(ENCODED_IMG_PATH);
            Bitmap encodedBitmap = BitmapFactory.decodeStream(encodedStream);
            Bitmap resBitmap = Flow.executeAndroidFlow(encodedBitmap);

            InputStream expectedStream = appContext.getAssets().open(ORIGINAL_IMG_PATH);
            Bitmap expectedBitmap = BitmapFactory.decodeStream(expectedStream);
            for (int row = 0 ; row < expectedBitmap.getHeight(); row++) {
                for (int col = 0; col < expectedBitmap.getWidth(); col++) {
                    int expectedPixel = expectedBitmap.getPixel(col, row);
                    //assert resBitmap != null;
                    int resPixel = resBitmap.getPixel(col, row);
                    if (row==149 && col ==5){
                        Log.d("testAndroidFlow", String.format("ExpectedPixel: %d, resPixel: %d", expectedPixel, resPixel));
                    }
                    assertEquals(String.format("Wrong pixel at row: %d, col: %d", row, col,
                            resPixel), expectedPixel, resPixel);
                }
            }

        } catch (NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }
}

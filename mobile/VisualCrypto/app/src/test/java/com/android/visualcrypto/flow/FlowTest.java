package com.android.visualcrypto.flow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import static org.junit.Assert.assertTrue;

public class FlowTest {

    private final String ENCODED_IMG_PATH = "src/main/assets/encoded_test.jpg";
    private final String ORIGINAL_IMG_PATH = "src/main/assets/encoded_test.jpg";

    @Test
    public void testFlow() throws FileNotFoundException {
        File f = new File(ENCODED_IMG_PATH);
        try {
            Bitmap shouldOriginalBitmap = Flow.executeAndroidFlow(f);
            Bitmap originalBitmap = BitmapFactory.decodeFile(ORIGINAL_IMG_PATH);
            assertTrue(shouldOriginalBitmap.sameAs(originalBitmap));

        } catch (NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}

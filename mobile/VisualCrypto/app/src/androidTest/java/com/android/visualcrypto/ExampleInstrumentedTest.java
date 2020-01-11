package com.android.visualcrypto;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.*;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.android.visualcrypto", appContext.getPackageName());
    }

    @Test
    public void testDeshuffle(){
        IvParameterSpec iv = Encryptor.generateIv(12);

        byte[] imgBytes = new byte[5120000];
        new SecureRandom().nextBytes(imgBytes);

        byte[] shuffledBytes = Shuffle.shuffleImgBytes(imgBytes, iv);
        byte[] deshuffledBytes = Deshuffle.getDeshuffledBytes(shuffledBytes, iv);

        assertArrayEquals(imgBytes, deshuffledBytes);
    }
}

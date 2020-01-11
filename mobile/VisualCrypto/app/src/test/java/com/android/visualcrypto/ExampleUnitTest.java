package com.android.visualcrypto;


import com.pc.encryptorDecryptor.decryptor.Decryptor;

import org.junit.Test;


import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import static org.junit.Assert.*;

import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testDecryptImage() throws Exception {
        byte[] imgBytes = new byte[5120000];
        new SecureRandom().nextBytes(imgBytes);

        SecretKey skey = Encryptor.generateSymmetricKey();
        SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");

        byte[] iv = new byte[] {1,2,3,4,5,6,7,8,9,10,11,12};
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        byte[] res1 = Decryptor.decryptImage(imgBytes, skeySpec , ivSpec);
        assertFalse(Arrays.equals(res1, imgBytes));
        byte[] res2 = Decryptor.decryptImage(res1, skeySpec , ivSpec);
        assertArrayEquals(res2, imgBytes);

    }
}
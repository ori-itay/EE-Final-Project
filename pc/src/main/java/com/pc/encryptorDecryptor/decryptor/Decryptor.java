package com.pc.encryptorDecryptor.decryptor;


import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.pc.encryptorDecryptor.EncryptorDecryptor;

public class Decryptor {
	
	
	public static byte[] decryptImage(byte[] encryptedImgBytes, SecretKeySpec skeySpec, IvParameterSpec iv) throws Exception {
		final byte[] xorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
		final byte[] imgBytes = EncryptorDecryptor.xorPaddedImage(encryptedImgBytes, xorBytes);
		return imgBytes;
	}
}

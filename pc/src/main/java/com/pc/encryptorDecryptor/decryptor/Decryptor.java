package com.pc.encryptorDecryptor.decryptor;


import com.pc.encryptorDecryptor.EncryptorDecryptor;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This module is used in the Android, as a library
 */
public class Decryptor {

	/**
	 * Decrypts the image data
	 * @param encryptedImgBytes - The encrypted image bytes
	 * @param skeySpec - The secret key spec
	 * @param iv - The IV
	 * @return the decrypted image bytes
	 */
	public static byte[] decryptImage(byte[] encryptedImgBytes, SecretKeySpec skeySpec, IvParameterSpec iv)  {
		final byte[] xorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
		final byte[] imgBytes = EncryptorDecryptor.xorPaddedImage(encryptedImgBytes, xorBytes);
		return imgBytes;
	}
}

package com.pc.encryptorDecryptor.decryptor;


import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.pc.encryptorDecryptor.EncryptorDecryptor;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Decryptor {
	
	
	public static byte[] decryptImage(byte[] encryptedImgBytes, SecretKeySpec skeySpec, IvParameterSpec iv) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
		final byte[] xorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
		final byte[] imgBytes = EncryptorDecryptor.xorPaddedImage(encryptedImgBytes, xorBytes);
		return imgBytes;
	}
}

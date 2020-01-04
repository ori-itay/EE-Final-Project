package com.pc.encryptor;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.pc.configuration.Constants.maxImageSizeBytes;

public class Encryptor {
	private static final String encryptionAlgorithm = "AES";
	private static final String encryptionMethod = encryptionAlgorithm + "/CBC/PKCS5Padding";
	private static final int symmetricKeyLength = 256;

	public SecretKey generateSymmetricKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGen;
		keyGen = KeyGenerator.getInstance(encryptionAlgorithm);
		keyGen.init(symmetricKeyLength);
		return keyGen.generateKey();
	}

	public byte[] xorPaddedImage(byte[] image) {

		return new byte[1];
	}

	public byte[] generateXorBytes(SecretKey skey, IvParameterSpec iv) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), encryptionAlgorithm);
		Cipher cipher = Cipher.getInstance(encryptionMethod);
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
		byte[] randomBytes = new byte[maxImageSizeBytes];
		/*
		 * for (int i = 0; i < maxImageSizeBytes/16TODO: value? ; i++) { byte[] iBytes =
		 * ByteBuffer.allocate(4).putInt(i).array(); // convert i to byte[4]
		 * 
		 * 
		 * } byte[] encryptedBytes = cipher.doFinal(iv+i);
		 */

		return new byte[1];
	}

	public IvParameterSpec generateIv(int ivLength) {
		byte[] iv = new byte[ivLength];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}
}

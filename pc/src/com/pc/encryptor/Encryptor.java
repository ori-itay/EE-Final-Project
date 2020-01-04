package com.pc.encryptor;

import static com.pc.configuration.Constants.maxImageSizeBytes;
import static com.pc.configuration.Constants.ivLength;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor {
	private static final String encryptionAlgorithm = "AES";
	private static final String encryptionMethod = encryptionAlgorithm + "/CBC/NoPadding";
	private static final int symmetricKeyLength = 256;
	private static final int intSize = 4;

	public static SecretKey generateSymmetricKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGen;
		keyGen = KeyGenerator.getInstance(encryptionAlgorithm);
		keyGen.init(symmetricKeyLength);
		return keyGen.generateKey();
	}

	public static byte[] xorPaddedImage(byte[] imageBytes, byte[] generatedXorBytes) {
		byte[] xoredImage = new byte[maxImageSizeBytes];
		for (int i = 0; i < maxImageSizeBytes; i++) {
			xoredImage[i] = (byte) (imageBytes[i] ^ generatedXorBytes[i]);
		}

		return xoredImage;
	}

	public static byte[] generateXorBytes(SecretKey skey, IvParameterSpec iv) throws Exception { 
		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), encryptionAlgorithm);
		Cipher cipher = Cipher.getInstance(encryptionMethod);
		byte[] cipherIV = new byte[16];
		System.arraycopy(iv.getIV(), 0, cipherIV, 0, ivLength);
		
		byte[] ourIV = iv.getIV();
		byte[] randomBytes = new byte[maxImageSizeBytes];
		int ivAndCounterLen = ourIV.length + intSize;
		byte[] ivAndCounter = new byte[ivAndCounterLen];
		
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(cipherIV));
		
		ExecutorService executor = Executors.newCachedThreadPool();
		
		for (int i = 0; i < maxImageSizeBytes/ivAndCounterLen ; i++) {
			byte[] counterBytes = ByteBuffer.allocate(intSize).putInt(i).array(); // convert i to byte[4]
			System.arraycopy(ourIV, 0, ivAndCounter, 0, ourIV.length);
			System.arraycopy(counterBytes, 0, ivAndCounter, ourIV.length, counterBytes.length); // concatenate iv||counter
						
			executor.execute(new ComputeAESBlock(randomBytes, i * ivAndCounterLen, ivAndCounter, cipher)); //AES[key,iv] (iv+i)
		} 
		return randomBytes;
	}

	public static IvParameterSpec generateIv(int ivLength) {
		byte[] iv = new byte[ivLength];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}
	
	
	private static class ComputeAESBlock implements Runnable {
		
		private final byte[] encryptedBytes;
		private final int startIndex;
		private final byte[] ivAndCounter;
		private final Cipher cipher;
		
		public ComputeAESBlock(byte[] encryptedBytes, int startIndex, byte[] ivAndCounter, Cipher cipher) {
			this.encryptedBytes = encryptedBytes;
			this.startIndex = startIndex;
			this.ivAndCounter = ivAndCounter;
			this.cipher = cipher;
		}
		
		@Override
		public void run() {
			try {
				byte[] encryptedBlockBytes = cipher.doFinal(ivAndCounter);
				System.arraycopy(encryptedBlockBytes, 0, encryptedBytes, startIndex, encryptedBlockBytes.length);
			} catch (BadPaddingException | IllegalBlockSizeException e) {
				e.printStackTrace();
			} 
		}
	}
}

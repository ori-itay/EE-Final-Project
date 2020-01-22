package com.pc.encryptorDecryptor;

import static com.pc.configuration.Parameters.ivLength;
import static com.pc.configuration.Parameters.MAX_ENCODED_LENGTH_BYTES;

import java.nio.ByteBuffer;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;

import javax.crypto.Cipher;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptorDecryptor {
	public static final String encryptionAlgorithm = "AES";
	public static final int symmetricKeyLength = 256;
	private static final int intSize = 4;
	private static final String encryptionMethod = encryptionAlgorithm + "/CBC/NoPadding";
	
	public static byte[] xorPaddedImage(byte[] imageBytes, byte[] generatedXorBytes) {
		final byte[] xoredImage = new byte[imageBytes.length];
		for (int i = 0; i < imageBytes.length; i++) {
			xoredImage[i] = (byte) (imageBytes[i] ^ generatedXorBytes[i]);
		}

		return xoredImage;
	}
	
	public static byte[] generateXorBytes(SecretKeySpec skeySpec, IvParameterSpec iv) throws Exception { 
		final Cipher cipher = Cipher.getInstance(encryptionMethod);
		final byte[] cipherIV = new byte[16];
		System.arraycopy(iv.getIV(), 0, cipherIV, 0, ivLength);
		
		final byte[] ourIV = iv.getIV();
		final byte[] randomBytes = new byte[MAX_ENCODED_LENGTH_BYTES];
		final int ivAndCounterLen = ourIV.length + intSize;
		final byte[] ivAndCounter = new byte[ivAndCounterLen];
		
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(cipherIV));
		
		//ExecutorService executor = Executors.newCachedThreadPool();
		
		for (int i = 0; i < MAX_ENCODED_LENGTH_BYTES/ivAndCounterLen ; i++) {
			final byte[] counterBytes = ByteBuffer.allocate(intSize).putInt(i).array(); // convert i to byte[4]
			System.arraycopy(ourIV, 0, ivAndCounter, 0, ourIV.length);
			System.arraycopy(counterBytes, 0, ivAndCounter, ourIV.length, counterBytes.length); // concatenate iv||counter
			//executor.execute(new ComputeAESBlock(randomBytes, i * ivAndCounterLen, ivAndCounter, cipher)); //AES[key,iv] (iv+i)
			
			final byte[] encryptedBlockBytes = cipher.update(ivAndCounter);
			System.arraycopy(encryptedBlockBytes, 0, randomBytes, i * ivAndCounterLen, encryptedBlockBytes.length);
			
		}	
		return randomBytes;
	}
	
//	private static class ComputeAESBlock implements Runnable {
//		
//		private final byte[] encryptedBytes;
//		private final int startIndex;
//		private final byte[] ivAndCounter;
//		private final Cipher cipher;
//		
//		public ComputeAESBlock(byte[] encryptedBytes, int startIndex, byte[] ivAndCounter, Cipher cipher) {
//			this.encryptedBytes = encryptedBytes;
//			this.startIndex = startIndex;
//			this.ivAndCounter = ivAndCounter;
//			this.cipher = cipher;
//		}
//		@Override
//		public void run() {
////			try {
////				byte[] encryptedBlockBytes = cipher.doFinal(ivAndCounter);
////				System.arraycopy(encryptedBlockBytes, 0, encryptedBytes, startIndex, encryptedBlockBytes.length);
////			} catch (BadPaddingException | IllegalBlockSizeException e) {
////				e.printStackTrace();
////			}
//			// option 2:
////			byte[] encryptedBlockBytes = cipher.update(ivAndCounter);
////			System.arraycopy(encryptedBlockBytes, 0, encryptedBytes, startIndex, encryptedBlockBytes.length);
//		}
//	}
}

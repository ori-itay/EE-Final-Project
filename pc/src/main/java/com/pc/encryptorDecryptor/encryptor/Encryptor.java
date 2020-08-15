package com.pc.encryptorDecryptor.encryptor;

import com.pc.encryptorDecryptor.EncryptorDecryptor;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static com.pc.encryptorDecryptor.EncryptorDecryptor.encryptionAlgorithm;

public class Encryptor {

	/**
	 * Generates symmetric secret key
	 * @return the key generated
	 */
	public static SecretKey generateSymmetricKey() throws NoSuchAlgorithmException {
		final KeyGenerator keyGen;
		keyGen = KeyGenerator.getInstance(encryptionAlgorithm);
		keyGen.init(EncryptorDecryptor.symmetricKeyLength);
		return keyGen.generateKey();
	}

	/**
	 * Generated a random IV
	 * @param ivLength - The length of the IV to be generated
	 * @return a random IV
	 */
	public static IvParameterSpec generateIv(int ivLength) {
		final byte[] iv = new byte[ivLength];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}

	/**
	 * Encrypts the image data
	 * @param imageBytes - The image bytes
	 * @param generatedXorBytes - The generated pseudo random bytes
	 * @return the encrypted image bytes
	 */
	public static byte[] encryptImage(byte[] imageBytes, byte[] generatedXorBytes) {
		return EncryptorDecryptor.xorPaddedImage(imageBytes, generatedXorBytes);
	}
}

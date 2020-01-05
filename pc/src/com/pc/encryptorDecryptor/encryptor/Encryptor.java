package com.pc.encryptorDecryptor.encryptor;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import com.pc.encryptorDecryptor.EncryptorDecryptor;

import static com.pc.encryptorDecryptor.EncryptorDecryptor.encryptionAlgorithm;

public class Encryptor {

	public static SecretKey generateSymmetricKey() throws NoSuchAlgorithmException {
		final KeyGenerator keyGen;
		keyGen = KeyGenerator.getInstance(encryptionAlgorithm);
		keyGen.init(EncryptorDecryptor.symmetricKeyLength);
		return keyGen.generateKey();
	}

	public static IvParameterSpec generateIv(int ivLength) {
		final byte[] iv = new byte[ivLength];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}
}

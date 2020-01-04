package test.encryptor;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;
import static com.pc.configuration.Constants.ivLength; 
import static com.pc.configuration.Constants.maxImageSizeBytes;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import  com.pc.encryptor.Encryptor;

@DisplayName("Encryptor Tests")
public class EncryptorTest {
	
	private static IvParameterSpec ivA;
	private static SecretKey skeyA;
	
	@BeforeAll
	public static void initVars() throws NoSuchAlgorithmException {
		ivA = Encryptor.generateIv(ivLength);
		skeyA = Encryptor.generateSymmetricKey();
	}
	
	@Test
	public void testGenerateIv() {
		IvParameterSpec specB = Encryptor.generateIv(ivLength);
		assertFalse(Arrays.equals(ivA.getIV(), specB.getIV()));
		IvParameterSpec specC = Encryptor.generateIv(8);
		IvParameterSpec specD = Encryptor.generateIv(24);
		assertTrue(specC.getIV().length == 8);
		assertTrue(specD.getIV().length == 24);
	}
	
	@Test
	public void testGenerateSymmetricKey() throws NoSuchAlgorithmException {
		assertTrue(skeyA.getEncoded().length == 32); // 256 bits
		
		SecretKey skeyB = Encryptor.generateSymmetricKey();
		assertFalse(Arrays.equals(skeyA.getEncoded(), skeyB.getEncoded()));
	}
	
	
	@Test
	public void testGenerateXorBytes() throws Exception {
		IvParameterSpec ivB = Encryptor.generateIv(ivLength);
		byte[] generatedXorBytesA = Encryptor.generateXorBytes(skeyA, ivA);
		assertTrue(generatedXorBytesA.length == maxImageSizeBytes);
		
		byte[] generatedXorBytesB = Encryptor.generateXorBytes(skeyA, ivB);
		assertFalse(Arrays.equals(generatedXorBytesA, generatedXorBytesB));
	}
	
	@Test
	public void testXorPaddedImage() throws Exception {
		SecretKey skeyB = Encryptor.generateSymmetricKey();
		IvParameterSpec ivB = Encryptor.generateIv(ivLength);
		byte[] generatedXorBytes = Encryptor.generateXorBytes(skeyB, ivB);
		byte[] imageBytes = new byte[maxImageSizeBytes];
		byte[] xoredImage = Encryptor.xorPaddedImage(generatedXorBytes, imageBytes);
		
		assertTrue(Arrays.equals(xoredImage, generatedXorBytes)); // a XOR 0 = a
	}
}

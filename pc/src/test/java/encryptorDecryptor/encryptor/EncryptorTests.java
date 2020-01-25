package encryptorDecryptor.encryptor;


import org.junit.jupiter.api.Test;

import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

//import static org.junit.jupiter.api.Assertions.*;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Encryptor Tests")
public class EncryptorTests {
	
	private static IvParameterSpec ivA;
	private static SecretKey skeyA;
	
	@BeforeAll
	public static void initVars() throws NoSuchAlgorithmException {
		ivA = Encryptor.generateIv(Parameters.ivLength);
		skeyA = Encryptor.generateSymmetricKey();
	}
	
	@Test
	public void testGenerateIv() {
		IvParameterSpec specB = Encryptor.generateIv(Parameters.ivLength);
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
		//byte[] ivBytes = new byte[] {1,1,1,1,1,1,1,1,1,1,1,1};
		//IvParameterSpec permIv = new IvParameterSpec(ivBytes);
		
		//SecretKeySpec permKeySpec = new SecretKeySpec("abcdefghijklmnop".getBytes(), "AES");
		
		SecretKeySpec skeySpec = new SecretKeySpec(skeyA.getEncoded(), "AES");
		
		
		byte[] generatedXorBytesA = EncryptorDecryptor.generateXorBytes(skeySpec, ivA);
		assertTrue(generatedXorBytesA.length == Constants.MAX_ENCODED_LENGTH_BYTES);
		
		byte[] generatedXorBytesC = EncryptorDecryptor.generateXorBytes(skeySpec, ivA);
		assertTrue(Arrays.equals(generatedXorBytesA, generatedXorBytesC));
		
		
		IvParameterSpec ivB = Encryptor.generateIv(Parameters.ivLength);
		byte[] generatedXorBytesB = EncryptorDecryptor.generateXorBytes(skeySpec, ivB); 
		assertFalse(Arrays.equals(generatedXorBytesA, generatedXorBytesB)); 
		 
	}
	
	@Test
	public void testXorPaddedImage() throws Exception {
		SecretKeySpec skeyB = new SecretKeySpec(Encryptor.generateSymmetricKey().getEncoded(), "AES");
		IvParameterSpec ivB = Encryptor.generateIv(Parameters.ivLength);
		byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeyB, ivB);
		byte[] imageBytes = new byte[Constants.MAX_ENCODED_LENGTH_BYTES];
		byte[] xoredImage = Encryptor.encryptImage(generatedXorBytes, imageBytes);
		
		assertTrue(Arrays.equals(xoredImage, generatedXorBytes)); // a XOR 0 = a
	}
}

package encryptorDecryptor.decryptor;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;


@DisplayName("Decryptor Tests")
public class DecryptorTests {

	@Test
	public void testDecryptImage() throws Exception {
		byte[] imgBytes = new byte[Constants.MAX_ENCODED_LENGTH_BYTES];
		new SecureRandom().nextBytes(imgBytes);
		
		SecretKey skey = Encryptor.generateSymmetricKey();
		IvParameterSpec iv = Encryptor.generateIv(Parameters.ivLength);
		SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), "AES");
		
		byte[] xorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
		byte[] encryptedImg = EncryptorDecryptor.xorPaddedImage(imgBytes, xorBytes); 
		
		
		byte[] decryptedImg = Decryptor.decryptImage(encryptedImg, skeySpec, iv);
		
		assertTrue(Arrays.equals(decryptedImg, imgBytes));	
	}
}
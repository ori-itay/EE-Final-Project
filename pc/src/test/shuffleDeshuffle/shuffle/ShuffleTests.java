package test.shuffleDeshuffle.shuffle;

import static com.pc.configuration.Constants.maxImageSizeBytes;
import static org.junit.jupiter.api.Assertions.*;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.spec.IvParameterSpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pc.configuration.Constants;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

@DisplayName("Shuffle Tests")
public class ShuffleTests {
	
	@Test
	public void testShuffleImgBytes() {
		IvParameterSpec iv = Encryptor.generateIv(Constants.ivLength);
		
		byte[] imgBytes = new byte[maxImageSizeBytes];
		new SecureRandom().nextBytes(imgBytes);
		
		byte[] shuffledBytes = Shuffle.shuffleImgBytes(imgBytes, iv);
		assertFalse(Arrays.equals(shuffledBytes, imgBytes));		
	}
}

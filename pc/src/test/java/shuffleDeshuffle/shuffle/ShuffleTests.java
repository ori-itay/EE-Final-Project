package shuffleDeshuffle.shuffle;


import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.spec.IvParameterSpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("Shuffle Tests")
public class ShuffleTests {
	
	@Test
	public void testShuffleImgBytes() {
		IvParameterSpec iv = Encryptor.generateIv(Parameters.ivLength);
		
		byte[] imgBytes = new byte[Constants.MAX_ENCODED_LENGTH_BYTES];
		new SecureRandom().nextBytes(imgBytes);
		
		byte[] shuffledBytes = Shuffle.shuffleImgBytes(imgBytes, iv);
		assertFalse(Arrays.equals(shuffledBytes, imgBytes), "d");
	}
}

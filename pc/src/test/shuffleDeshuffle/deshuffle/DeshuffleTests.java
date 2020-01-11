package test.shuffleDeshuffle.deshuffle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.security.SecureRandom;

import javax.crypto.spec.IvParameterSpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pc.configuration.Constants;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

@DisplayName("Deshuffle Tests")
public class DeshuffleTests {

	@Test
	public void testDeshuffleBytes() {
		IvParameterSpec iv = Encryptor.generateIv(Constants.ivLength);
		
		byte[] imgBytes = new byte[Constants.MAX_ENCODED_LENGTH_BYTES];
		new SecureRandom().nextBytes(imgBytes);
		
		byte[] shuffledBytes = Shuffle.shuffleImgBytes(imgBytes, iv);
		byte[] deshuffledBytes = Deshuffle.getDeshuffledBytes(shuffledBytes, iv);

		assertArrayEquals(imgBytes, deshuffledBytes);
	}
}

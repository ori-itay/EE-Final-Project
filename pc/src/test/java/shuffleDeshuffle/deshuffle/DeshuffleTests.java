package shuffleDeshuffle.deshuffle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.security.SecureRandom;

import javax.crypto.spec.IvParameterSpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

@DisplayName("Deshuffle Tests")
public class DeshuffleTests {
	/**
	 * Checks that shuffles and deshuffles result in the original image byte array on basic pixel array
	 */
//	@Test
//	public void testDeshufflePixels() {
//		IvParameterSpec iv = Encryptor.generateIv(Parameters.ivLength);
//
//		if (Constants.CHANNELS == 4) {
//			byte[] imgBytes = {1,2,3,4,      5,6,7,8,      9,10,11,12,   13,14,15,16};
//
//			byte[] shuffledBytes = Shuffle.shuffleImgPixels(imgBytes, iv);
//			byte[] deshuffledBytes = Deshuffle.getDeshuffledBytes(shuffledBytes, iv);
//
//			assertArrayEquals(imgBytes, deshuffledBytes);
//		} else if (Constants.CHANNELS == 3) {
//			//byte[] imgBytes = {1,2,3,      5,6,7,      9,10,11,   13,14,15};
//			byte[] imgBytes = {1,2,3,4,5,	6,7,8,	9,10,11,	12,13,14,	15,16,17,	18,19,20     ,21,22};
//			byte[] shuffledBytes = Shuffle.shuffleImgPixels(imgBytes, iv);
//			byte[] deshuffledBytes = Deshuffle.getDeshuffledBytes(shuffledBytes, iv);
//
//			assertArrayEquals(imgBytes, deshuffledBytes);
//		}
//	}
}

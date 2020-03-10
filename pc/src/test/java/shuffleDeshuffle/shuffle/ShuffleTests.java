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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Shuffle Tests")
public class ShuffleTests {

	/**
	 * Checks pixel shuffle on basic array
	 */
//	@Test
//	public void testPixelShuffle() {
//		if (Constants.CHANNELS == 4) {
//			byte[] bytearr = {1,0,2,0,    0,5,0,6,    3,3,3,3};
//			byte[] iv = {1,0,0,0,0,0,0,0,0,0,0,1};
//			IvParameterSpec ivSpec = new IvParameterSpec(iv);
//			byte[] shuffledPixels  = Shuffle.shuffleImgPixels(bytearr, ivSpec);
//
//			byte[] result = {0,5,0,6,     1,0,2,0,     3,3,3,3};
//			assertArrayEquals(result, shuffledPixels);
//		}
//		else if (Constants.CHANNELS == 3) {
//			byte[] bytearr = {1,0,2,    0,5,0,    3,3,3};
//			byte[] iv = {1,0,0,0,0,0,0,0,0,0,0,1};
//			IvParameterSpec ivSpec = new IvParameterSpec(iv);
//			byte[] shuffledPixels  = Shuffle.shuffleImgPixels(bytearr, ivSpec);
//
//			byte[] result = {0,5,0,     1,0,2,     3,3,3};
//			assertArrayEquals(result, shuffledPixels);
//		}
//
//	}
}

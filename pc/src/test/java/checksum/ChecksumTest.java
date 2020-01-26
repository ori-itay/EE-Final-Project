package checksum;


import com.pc.checksum.Checksum;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
//import org.junit.jupiter.params.ParameterizedTest;


public class ChecksumTest {

//	@Test
//	@ParameterizedTest
//	@ValueSource(ints = {1, 36, 50, 240})
//	public void testComputeChecksum(int width) {
//		SecureRandom secureRandom = new SecureRandom();
//		secureRandom.setSeed(1);
//
//		int height = 34;
//		byte[] checksum = Checksum.computeChecksum(ByteBuffer.allocate(8).putInt(width).putInt(height).array());
//
//		assertTrue(Checksum.isValidChecksum(width, height, checksum));
//
//		height += 1;
//		assertFalse(Checksum.isValidChecksum(width,  height, checksum));
//	}
}

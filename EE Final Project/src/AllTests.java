import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class AllTests {

	@Test
	void test() {
		//fail("Not yet implemented");
		assertEquals(1, 1);
	}
	
	@Test
	void testByteArrayToInt() {
		byte[] byteArray = {1,2,3};
		int returnVal = DisplayDecoder.fromByteArray(byteArray);
		assertEquals(returnVal, 66051);
	}
	
	
	@Test
	void testEncodeDecodeIdempotent() throws Exception {
		
		String testData = "BLABLA";
		BufferedImage encodedImage = DisplayEncoder.encodeBytes(testData);
		String decodedString = DisplayDecoder.decodeImage(encodedImage);
		
		assertEquals(testData.length(), decodedString.length());
		assertEquals(testData, decodedString);
	}

}

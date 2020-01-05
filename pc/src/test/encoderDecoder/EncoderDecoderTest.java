package test.encoderDecoder;

import static org.junit.jupiter.api.Assertions.*;
import static com.pc.configuration.Constants.*;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;

public class EncoderDecoderTest {
	
	@Test
	public void testByteArrayToInt() {
		byte[] byteArray = {1,2,3};
		int returnVal = DisplayDecoder.TestByteArrayToInt(byteArray);
		assertEquals(returnVal, 197121);
	}
	
	@Test
	public void testEncodeDecodeEncodeIdempotent() throws Exception {
		
		//String testData = "BLABLABLABLABLA";
		int length = MAX_ENCODED_LENGTH;
		//int length = 1229310 + 1;
		byte[] byteArr = new byte [length/8];
		Random rand = new Random(); 
		for(int i = 0; i < byteArr.length; i++) { byteArr[i] = (byte) rand.nextInt(127);}
		//Arrays.fill(byteArr, 0, byteArr.length -1, (byte) 127);
		String testData = new String(byteArr);
		BufferedImage encodedImage = DisplayEncoder.encodeBytes(testData.getBytes());
		//save encoded image
		String path = "C:\\Users\\user\\Downloads\\qrcode.png";
		File encodedFile = new File(path);
		ImageIO.write(encodedImage, "png", encodedFile);
		String decodedString = new String(DisplayDecoder.decodeFilePC(encodedFile).getDecodedData());
		System.out.println(decodedString);
		
		assertEquals(testData.length(), decodedString.length());
		assertEquals(testData, decodedString);
		
		BufferedImage newEncodedImage = DisplayEncoder.encodeBytes(decodedString.getBytes());

		assertEquals(newEncodedImage.getWidth(), encodedImage.getWidth());
		assertEquals(newEncodedImage.getHeight(), encodedImage.getHeight());
    	for (int row=0 ; row < newEncodedImage.getHeight() ; row++) {
    		for (int col=0; col < newEncodedImage.getWidth(); col++) {
    			assertEquals (newEncodedImage.getRGB(col,  row) , encodedImage.getRGB(col,  row));
    		}
    	}
	}
	
	@Test
	public void testEncodeDecodeEncodeRotatedIdempotent() throws Exception {
		
		String testData = "BLABLA";
		BufferedImage encodedImage = null;
		BufferedImage newEncodedImage = null;
		int rotation;
		
		for(rotation = 0; rotation <360; rotation+=90)
			//encode data to image
			encodedImage = DisplayEncoder.encodeBytes(testData.getBytes());
			
			//rotate encoded image
			final double rads = Math.toRadians(rotation);
			final double sin = Math.abs(Math.sin(rads));
			final double cos = Math.abs(Math.cos(rads));
			final int w = (int) Math.floor(encodedImage.getWidth() * cos + encodedImage.getHeight() * sin);
			final int h = (int) Math.floor(encodedImage.getHeight() * cos + encodedImage.getWidth() * sin);
			final BufferedImage rotatedImage = new BufferedImage(w, h, encodedImage.getType());
			final AffineTransform at = new AffineTransform();
			at.translate(w / 2, h / 2);
			at.rotate(rads,0, 0);
			at.translate(-encodedImage.getWidth() / 2, -encodedImage.getHeight() / 2);
			final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
			rotateOp.filter(encodedImage,rotatedImage);
	
			//save rotated encoded image
			String rotatedPath = "C:\\Users\\user\\Downloads\\rotated_qrcode.png";
			File rotatedFile = new File(rotatedPath);
			ImageIO.write(rotatedImage, "png", rotatedFile);
			
			//read and decode
			File newRotatedFile = null;
   			try {newRotatedFile = new File(rotatedPath);}	
			catch(Exception NullPointerException){System.out.println("Entered input filepath doesn't exist.\n");}
			String decodedString = new String(DisplayDecoder.decodeFilePC(newRotatedFile).getDecodedData());			
			//assert decode(encode(data)) == data
			assertEquals(testData.length(), decodedString.length());
			assertEquals(testData, decodedString);
			
			//assert encode(decode(data)) == encode(data)
			newEncodedImage = DisplayEncoder.encodeBytes(decodedString.getBytes());
			assertEquals(newEncodedImage.getWidth(), encodedImage.getWidth());
			assertEquals(newEncodedImage.getHeight(), encodedImage.getHeight());
	    	for (int row=0 ; row < newEncodedImage.getHeight() ; row++) {
	    		for (int col=0; col < newEncodedImage.getWidth(); col++) {
	    			assertEquals (newEncodedImage.getRGB(col,  row) , encodedImage.getRGB(col,  row));
	    		}
	    	}
	}
	
	@Test
	public void testEncodeLongest() throws Exception {
		
	}
}

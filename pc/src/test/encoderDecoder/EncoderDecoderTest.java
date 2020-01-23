package test.encoderDecoder;

import static org.junit.jupiter.api.Assertions.*;
import static com.pc.configuration.Constants.*;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;

public class EncoderDecoderTest {
	
//	@Ignore
//	@Test
//	public void testByteArrayToInt() {
//		byte[] byteArray = {1,2,3};
//		int returnVal = DisplayDecoder.TestByteArrayToInt(byteArray);
//		assertEquals(returnVal, 197121);
//	}
//	
//	@Ignore
//	@Test
//	public void testEncodeDecodeEncodeIdempotentLongest() throws Exception {
//		
//		int length = MAX_ENCODED_LENGTH_BYTES;
//		byte[] byteArr = new byte [length/BITS_IN_BYTE];
//		Random rand = new Random(); 
//		for(int i = 0; i < byteArr.length; i++) { byteArr[i] = (byte) rand.nextInt(127);}
//		//Arrays.fill(byteArr, 0, byteArr.length -1, (byte) 127);
//		String testData = new String(byteArr);
//		BufferedImage encodedImage = DisplayEncoder.encodeBytes(testData.getBytes());
//		//save encoded image
//		String path = "C:\\Users\\user\\Downloads\\qrcode.png";
//		File encodedFile = new File(path);
//		ImageIO.write(encodedImage, "png", encodedFile);
//		String decodedString = new String(DisplayDecoder.decodeFilePC(encodedFile).getDecodedData());		
//		assertEquals(testData.length(), decodedString.length());
//		assertEquals(testData, decodedString);
//		
//		BufferedImage newEncodedImage = DisplayEncoder.encodeBytes(decodedString.getBytes());
//
//		assertEquals(newEncodedImage.getWidth(), encodedImage.getWidth());
//		assertEquals(newEncodedImage.getHeight(), encodedImage.getHeight());
//    	for (int row=0 ; row < newEncodedImage.getHeight() ; row++) {
//    		for (int col=0; col < newEncodedImage.getWidth(); col++) {
//    			assertEquals (newEncodedImage.getRGB(col,  row) , encodedImage.getRGB(col,  row));
//    		}
//    	}
//	}
//	
//	@Ignore
//	@Test
//	public void testEncodeDecodeEncodeRotatedIdempotent() throws Exception {
//		
//		String testData = "BLABLA";
//		byte[] byteArr = new byte [MAX_ENCODED_LENGTH/BITS_IN_BYTE];
//		Random rand = new Random(); 
//		byte[] argByteArr = testData.getBytes();
//		for(int i = 0; i < argByteArr.length; i++) { byteArr[i] = argByteArr[i];}
//		for(int i = testData.length(); i < byteArr.length; i++) { byteArr[i] = (byte) rand.nextInt(127);} //pad with random vals 
//		//for(int i = argument.length(); i < byteArr.length; i++) { byteArr[i] = (byte) 0;}
//		
//		BufferedImage encodedImage = null;
//		BufferedImage newEncodedImage = null;
//		int rotation;
//		
//		for(rotation = 0; rotation <360; rotation+=90)
//			//encode data to image
//			encodedImage = DisplayEncoder.encodeBytes(byteArr);
//			
//			//rotate encoded image
//			final double rads = Math.toRadians(rotation);
//			final double sin = Math.abs(Math.sin(rads));
//			final double cos = Math.abs(Math.cos(rads));
//			final int w = (int) Math.floor(encodedImage.getWidth() * cos + encodedImage.getHeight() * sin);
//			final int h = (int) Math.floor(encodedImage.getHeight() * cos + encodedImage.getWidth() * sin);
//			final BufferedImage rotatedImage = new BufferedImage(w, h, encodedImage.getType());
//			final AffineTransform at = new AffineTransform();
//			at.translate(w / 2, h / 2);
//			at.rotate(rads,0, 0);
//			at.translate(-encodedImage.getWidth() / 2, -encodedImage.getHeight() / 2);
//			final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
//			rotateOp.filter(encodedImage,rotatedImage);
//	
//			//save rotated encoded image
//			String rotatedPath = "C:\\Users\\user\\Downloads\\rotated_qrcode.png";
//			File rotatedFile = new File(rotatedPath);
//			ImageIO.write(rotatedImage, "png", rotatedFile);
//			
//			//read and decode
//			File newRotatedFile = null;
//   			try {newRotatedFile = new File(rotatedPath);}	
//			catch(Exception NullPointerException){System.out.println("Entered input filepath doesn't exist.\n");}
//			String decodedString = new String(DisplayDecoder.decodeFilePC(newRotatedFile).getDecodedData());			
//			//assert decode(encode(data)) == data
//			assertEquals(byteArr.length, decodedString.length());
//			assertEquals(new String(byteArr), decodedString);
//			
//			//assert encode(decode(data)) == encode(data)
//			newEncodedImage = DisplayEncoder.encodeBytes(decodedString.getBytes());
//			assertEquals(newEncodedImage.getWidth(), encodedImage.getWidth());
//			assertEquals(newEncodedImage.getHeight(), encodedImage.getHeight());
//	    	for (int row=0 ; row < newEncodedImage.getHeight() ; row++) {
//	    		for (int col=0; col < newEncodedImage.getWidth(); col++) {
//	    			assertEquals (newEncodedImage.getRGB(col,  row) , encodedImage.getRGB(col,  row));
//	    		}
//	    	}
//	}
//	
//	
//	@ParameterizedTest
//	@ValueSource(strings = {"BLABLA","This is a short sentence for testing encoding and decoding and encoding again idempotent!"})
//	public void testEncodeDecodeEncodeIdempotentSimple(String argument) throws Exception {
//		
//		byte[] byteArr = new byte [MAX_ENCODED_LENGTH/BITS_IN_BYTE];
//		Random rand = new Random(); 
//		byte[] argByteArr = argument.getBytes();
//		for(int i = 0; i < argByteArr.length; i++) { byteArr[i] = argByteArr[i];}
//		for(int i = argument.length(); i < byteArr.length; i++) { byteArr[i] = (byte) rand.nextInt(127);} //pad with random vals 
//		//for(int i = argument.length(); i < byteArr.length; i++) { byteArr[i] = (byte) 0;}
//		BufferedImage encodedImage = DisplayEncoder.encodeBytes(byteArr);
//		//save encoded image
//		String path = "C:\\Users\\user\\Downloads\\qrcode.png";
//		File encodedFile = new File(path);
//		ImageIO.write(encodedImage, "png", encodedFile);
//		String decodedString = new String(DisplayDecoder.decodeFilePC(encodedFile).getDecodedData());
//		//System.out.println(decodedString);
//		
//		assertEquals(byteArr.length, decodedString.length());
//		assertEquals(new String(byteArr), decodedString);
//		
//		BufferedImage newEncodedImage = DisplayEncoder.encodeBytes(decodedString.getBytes());
//
//		assertEquals(newEncodedImage.getWidth(), encodedImage.getWidth());
//		assertEquals(newEncodedImage.getHeight(), encodedImage.getHeight());
//    	for (int row=0 ; row < newEncodedImage.getHeight() ; row++) {
//    		for (int col=0; col < newEncodedImage.getWidth(); col++) {
//    			assertEquals (newEncodedImage.getRGB(col,  row) , encodedImage.getRGB(col,  row));
//    		}
//    	}
//	}
//	
}

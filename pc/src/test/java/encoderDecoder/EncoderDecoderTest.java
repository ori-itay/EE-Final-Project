//package encoderDecoder;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static com.pc.configuration.Constants.*;
//
//import java.awt.geom.AffineTransform;
//import java.awt.image.AffineTransformOp;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.util.Random;
//
//import javax.imageio.ImageIO;
//
//import com.pc.cli.EncodeDecodeCLI;
//import com.pc.encoderDecoder.DisplayDecoder;
//import com.pc.encoderDecoder.DisplayEncoder;
//import org.junit.jupiter.api.Test;
//
//public class EncoderDecoderTest {
//
//
//	@Test
//	public void testEncodeDecodeEndToEndIdempotent() throws Exception {
//        byte[] const_key = new byte[] {100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115};
//
//        File encodedFile = new File("./encodedImage.jpg");
//        File decodedFile = new File("./decodedImage.jpg");
//        File newEncodedFile = new File("./newEncodedImage.jpg");
//        File newDecodedFile = new File("./newDecodedImage.jpg");
//        //original image
//        File origFile = new File("./200_200.jpg");
//        BufferedImage origImage = ImageIO.read(origFile);
//
//        //encode 1
//        BufferedImage encodedImage = EncodeDecodeCLI.executeEncodingProccess("./200_200.jpg", const_key);
//		ImageIO.write(encodedImage, "jpg", encodedFile);
//        //decode 1
//        BufferedImage decodedImage = EncodeDecodeCLI.executeDecodingProccess("./encodedImage.jpg", const_key);
//        ImageIO.write(decodedImage, "jpg", decodedFile);
//        //encode 2 (encode the decoded image)
//        BufferedImage newEncodedImage = EncodeDecodeCLI.executeEncodingProccess("./decodedImage.jpg", const_key);
//        ImageIO.write(encodedImage, "jpg", newEncodedFile);
//        //decode 2 (decode the new encoded image)
//        BufferedImage newDecodedImage = EncodeDecodeCLI.executeDecodingProccess("./encodedImage.jpg", const_key);
//        ImageIO.write(encodedImage, "jpg", newDecodedFile);
//
//		assertEquals(decodedImage.getWidth(), newDecodedImage.getWidth());
//        assertEquals(newDecodedImage.getHeight(), newDecodedImage.getHeight());
//        assertEquals(decodedImage.getWidth(), origImage.getWidth());
//        assertEquals(newDecodedImage.getHeight(), origImage.getHeight());
//    	for (int row=0 ; row < newDecodedImage.getHeight() ; row++) {
//    		for (int col=0; col < newDecodedImage.getWidth(); col++) {
//    			assertEquals (decodedImage.getRGB(col,  row) , newDecodedImage.getRGB(col,  row));
//                //assertEquals (origImage.getRGB(col,  row)<<8 , decodedImage.getRGB(col,  row)<<8); //checks only RGB (no alpha)
//    		}
//    	}
//	}
////
////	@Ignore
////	@Test
////	public void testEncodeDecodeEncodeRotatedIdempotent() throws Exception {
////
////		String testData = "BLABLA";
////		byte[] byteArr = new byte [MAX_ENCODED_LENGTH/BITS_IN_BYTE];
////		Random rand = new Random();
////		byte[] argByteArr = testData.getBytes();
////		for(int i = 0; i < argByteArr.length; i++) { byteArr[i] = argByteArr[i];}
////		for(int i = testData.length(); i < byteArr.length; i++) { byteArr[i] = (byte) rand.nextInt(127);} //pad with random vals
////		//for(int i = argument.length(); i < byteArr.length; i++) { byteArr[i] = (byte) 0;}
////
////		BufferedImage encodedImage = null;
////		BufferedImage newEncodedImage = null;
////		int rotation;
////
////		for(rotation = 0; rotation <360; rotation+=90)
////			//encode data to image
////			encodedImage = DisplayEncoder.encodeBytes(byteArr);
////
////			//rotate encoded image
////			final double rads = Math.toRadians(rotation);
////			final double sin = Math.abs(Math.sin(rads));
////			final double cos = Math.abs(Math.cos(rads));
////			final int w = (int) Math.floor(encodedImage.getWidth() * cos + encodedImage.getHeight() * sin);
////			final int h = (int) Math.floor(encodedImage.getHeight() * cos + encodedImage.getWidth() * sin);
////			final BufferedImage rotatedImage = new BufferedImage(w, h, encodedImage.getType());
////			final AffineTransform at = new AffineTransform();
////			at.translate(w / 2, h / 2);
////			at.rotate(rads,0, 0);
////			at.translate(-encodedImage.getWidth() / 2, -encodedImage.getHeight() / 2);
////			final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
////			rotateOp.filter(encodedImage,rotatedImage);
////
////			//save rotated encoded image
////			String rotatedPath = "C:\\Users\\user\\Downloads\\rotated_qrcode.png";
////			File rotatedFile = new File(rotatedPath);
////			ImageIO.write(rotatedImage, "png", rotatedFile);
////
////			//read and decode
////			File newRotatedFile = null;
////   			try {newRotatedFile = new File(rotatedPath);}
////			catch(Exception NullPointerException){System.out.println("Entered input filepath doesn't exist.\n");}
////			String decodedString = new String(DisplayDecoder.decodeFilePC(newRotatedFile).getDecodedData());
////			//assert decode(encode(data)) == data
////			assertEquals(byteArr.length, decodedString.length());
////			assertEquals(new String(byteArr), decodedString);
////
////			//assert encode(decode(data)) == encode(data)
////			newEncodedImage = DisplayEncoder.encodeBytes(decodedString.getBytes());
////			assertEquals(newEncodedImage.getWidth(), encodedImage.getWidth());
////			assertEquals(newEncodedImage.getHeight(), encodedImage.getHeight());
////	    	for (int row=0 ; row < newEncodedImage.getHeight() ; row++) {
////	    		for (int col=0; col < newEncodedImage.getWidth(); col++) {
////	    			assertEquals (newEncodedImage.getRGB(col,  row) , encodedImage.getRGB(col,  row));
////	    		}
////	    	}
////	}
////
////
////	@ParameterizedTest
////	@ValueSource(strings = {"BLABLA","This is a short sentence for testing encoding and decoding and encoding again idempotent!"})
////	public void testEncodeDecodeEncodeIdempotentSimple(String argument) throws Exception {
////
////		byte[] byteArr = new byte [MAX_ENCODED_LENGTH/BITS_IN_BYTE];
////		Random rand = new Random();
////		byte[] argByteArr = argument.getBytes();
////		for(int i = 0; i < argByteArr.length; i++) { byteArr[i] = argByteArr[i];}
////		for(int i = argument.length(); i < byteArr.length; i++) { byteArr[i] = (byte) rand.nextInt(127);} //pad with random vals
////		//for(int i = argument.length(); i < byteArr.length; i++) { byteArr[i] = (byte) 0;}
////		BufferedImage encodedImage = DisplayEncoder.encodeBytes(byteArr);
////		//save encoded image
////		String path = "C:\\Users\\user\\Downloads\\qrcode.png";
////		File encodedFile = new File(path);
////		ImageIO.write(encodedImage, "png", encodedFile);
////		String decodedString = new String(DisplayDecoder.decodeFilePC(encodedFile).getDecodedData());
////		//System.out.println(decodedString);
////
////		assertEquals(byteArr.length, decodedString.length());
////		assertEquals(new String(byteArr), decodedString);
////
////		BufferedImage newEncodedImage = DisplayEncoder.encodeBytes(decodedString.getBytes());
////
////		assertEquals(newEncodedImage.getWidth(), encodedImage.getWidth());
////		assertEquals(newEncodedImage.getHeight(), encodedImage.getHeight());
////    	for (int row=0 ; row < newEncodedImage.getHeight() ; row++) {
////    		for (int col=0; col < newEncodedImage.getWidth(); col++) {
////    			assertEquals (newEncodedImage.getRGB(col,  row) , encodedImage.getRGB(col,  row));
////    		}
////    	}
////	}
////
//}

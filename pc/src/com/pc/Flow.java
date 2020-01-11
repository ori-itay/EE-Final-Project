package com.pc;

import static com.pc.configuration.Constants.DIMENSIONS_ENCODING_BYTE_LEN;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import com.pc.configuration.Constants;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

public class Flow {
	
	private static final String encodedFilePath = "encodedImage.png";
	
	public static void main(String[] args) throws IOException {
		
		//load image
		File inputFile = new File("itay.jpeg");
		
		BufferedImage image = ImageIO.read(inputFile);
		byte[] imageBytes = FlowUtils.convertToBytesUsingGetRGB(image);
		
		IvParameterSpec iv = Encryptor.generateIv(Constants.ivLength); //maybe change to private static for CLI use?
		//SecretKey skey; 
		BufferedImage encodedImage;
		//RotatedImageSampler imageSampler;
		try {
			//skey = Encryptor.generateSymmetricKey();
			/* constant key */
			byte[] const_key = new byte[] {100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115};
			SecretKeySpec skeySpec = new SecretKeySpec(const_key, Constants.ENCRYPTION_ALGORITHM);
			/****************/
			//SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), Constants.ENCRYPTION_ALGORITHM);
			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
			
			byte[] encryptedImg = Encryptor.encryptImage(imageBytes, generatedXorBytes); //width+height need to be inside or would be unencrypted
			byte[] shuffledEncryptedImg = Shuffle.shuffleImgBytes(encryptedImg, iv);
			encodedImage = DisplayEncoder.encodeBytes(shuffledEncryptedImg, iv.getIV());
			ImageIO.write(encodedImage, "png", new File(encodedFilePath));
			/*
			 * imageSampler = DisplayDecoder.decodeFilePC(encodedImageFile); byte[]
			 * unShuffledEncryptedImg =
			 * Deshuffle.getDeshuffledBytes(imageSampler.getDecodedData(), iv); byte[]
			 * decryptedBytes = Decryptor.decryptImage(unShuffledEncryptedImg, skeySpec,
			 * iv); BufferedImage decodedImage = convertToImageUsingGetRGB(decryptedBytes,
			 * imageSampler.getImageWidth(), imageSampler.getImageWidth()); //correct
			 * dimensions assignments according to decision ImageIO.write(decodedImage,
			 * "jpeg", new File(decodedFilePath));
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void encryptImageBytes(byte[] imageBytes, String inputPath, String outputPath) {
		
	}
	
	public static void decryptImageBytes(byte[] encrytedBytes, String inputPath, String outputPath) {
		
	}
	
	
    public static BufferedImage convertToImageUsingGetRGB(byte[] imageData, int width, int height) {

		int index;
    	
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int channels = 4;
        int ARGB; 
        ByteBuffer wrapped;
        
        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
        	  index = (row*width + col)*channels;
        	  wrapped = ByteBuffer.wrap(imageData, DIMENSIONS_ENCODING_BYTE_LEN + index, channels);
        	  ARGB = wrapped.getInt();
        	  image.setRGB(col, row, ARGB);
           }
        }

        return image;
     }
}

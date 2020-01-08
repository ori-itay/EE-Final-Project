package com.pc;

import static com.pc.configuration.Constants.DIMENSIONS_ENCODING_BYTE_LEN;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import com.pc.configuration.Constants;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;

public class Flow {
	
	private static final String encodedFilePath = "encodedImage.jpeg";
	private static final String decodedFilePath = "decodedImage.jpeg";
	
	public static void main(String[] args) {
		byte[] imageBytes = null; // TODO: load image bytes..
		//load image
		File encodedImageFile;
		
		IvParameterSpec iv = Encryptor.generateIv(Constants.ivLength); //maybe change to private static for CLI use?
		SecretKey skey; //maybe change to private static for CLI use?
		BufferedImage encodedImage;
		RotatedImageSampler imageSampler;
		try {
			skey = Encryptor.generateSymmetricKey();
			SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), Constants.ENCRYPTION_ALGORITHM);
			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
			
			byte[] encryptedImg = Encryptor.encryptImage(imageBytes, generatedXorBytes); //width+height need to be inside or would be unencrypted
			byte[] shuffledEncryptedImg = Shuffle.shuffleImgBytes(encryptedImg, iv);
			encodedImage = DisplayEncoder.encodeBytes(shuffledEncryptedImg, iv.getIV());
			ImageIO.write(encodedImage, "jpeg", encodedImageFile= new File(encodedFilePath));
			imageSampler = DisplayDecoder.decodeFilePC(encodedImageFile);
			byte[] unShuffledEncryptedImg = Deshuffle.getDeshuffledBytes(imageSampler.getDecodedData(), iv);
			byte[] decryptedBytes = Decryptor.decryptImage(unShuffledEncryptedImg, skeySpec, iv);
			BufferedImage decodedImage = convertToImageUsingGetRGB(decryptedBytes, imageSampler.getImageWidth(), imageSampler.getImageWidth()); //correct dimensions assignments according to decision
			ImageIO.write(decodedImage, "jpeg", new File(decodedFilePath));
		} catch (Exception e) {
			// TODO Auto-generated catch block
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

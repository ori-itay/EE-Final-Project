package com.pc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
		
		IvParameterSpec iv = Encryptor.generateIv(Constants.ivLength); 
		byte[] chksumIV = getChecksum(iv); 
		//SecretKey skey; 
		BufferedImage encodedImage;
		try {
			//skey = Encryptor.generateSymmetricKey();
			/* constant key */
			byte[] const_key = new byte[] {100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115};
			SecretKeySpec skeySpec = new SecretKeySpec(const_key, Constants.ENCRYPTION_ALGORITHM);
			/****************/
			//SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), Constants.ENCRYPTION_ALGORITHM);
			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
			
			byte[] encryptedImg = Encryptor.encryptImage(imageBytes, generatedXorBytes);
			byte[] shuffledEncryptedImg = Shuffle.shuffleImgBytes(encryptedImg, iv);
			encodedImage = DisplayEncoder.encodeBytes(shuffledEncryptedImg, iv.getIV(),  chksumIV);
			ImageIO.write(encodedImage, "png", new File(encodedFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

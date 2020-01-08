package com.pc;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.pc.configuration.Constants;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

public class Flow {
	
	public static void main(String[] args) {
		byte[] imageBytes = null; // TODO: load image bytes..
		//load image
		
		IvParameterSpec iv = Encryptor.generateIv(Constants.ivLength);
		SecretKey skey;
		try {
			skey = Encryptor.generateSymmetricKey();
			SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), Constants.ENCRYPTION_ALGORITHM);
			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
			
			byte[] encryptedImg = Encryptor.encryptImage(imageBytes, generatedXorBytes);
			byte[] shuffledEncryptedImg = Shuffle.shuffleImgBytes(encryptedImg, iv);
			//encode(shuffledEncryptedImg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

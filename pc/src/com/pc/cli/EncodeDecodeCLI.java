package com.pc.cli;
import static com.pc.configuration.Constants.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import com.checksum.Checksum;
import com.pc.FlowUtils;
import com.pc.configuration.Constants;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;



public class EncodeDecodeCLI {
	
	public static void main(String... args) throws Exception {
		
		boolean continueProgram = true;
	    Scanner scanner = new Scanner(System.in);  // Create a Scanner object
	    File inputFile;
	    IvParameterSpec iv;
	    byte[] chksumIV;
	    
	    System.out.println("Enter Decode/Encode command:");
	    
	    while(continueProgram) {
	    	String userCommand = scanner.nextLine().toLowerCase();  // Read user input

    		String[] splitedCommand = userCommand.split("\\s+");
    		if(splitedCommand.length < 2 && !splitedCommand[0].equals("exit")) {
	    		System.out.println("Usage: 'Encode [filepath] [target encoded filepath]'\n"
	    				+ "Usage: 'EncodeString [String] [target encoded filepath]'\n"
	    				+ "Usage: 'Decode [encoded filepath]'\n"
	    				+ "Usage: 'DecodeString [encoded filepath]'\n"
	    				+ "Usage: 'Exit' to stop execution.");
    			continue;
    		}
    		else {  
    			/* constant key */
    			byte[] const_key = new byte[] {100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115};
    			SecretKeySpec skeySpec = new SecretKeySpec(const_key, Constants.ENCRYPTION_ALGORITHM);
    			/****************/
    			
    			if(splitedCommand[0].equals("encode") || splitedCommand[0].equals("encodestring")) {
    				byte[] rawData;
    				if(splitedCommand[0].equals("encode")) {
    	       			try {inputFile = new File(splitedCommand[1]);}	
            			catch(Exception NullPointerException){
            				System.out.println("Entered input filepath doesn't exist.\n");
            				continue;
            			}
    	       			BufferedImage image = ImageIO.read(inputFile);
    	       			rawData = FlowUtils.convertToBytesUsingGetRGB(image) ;
    				}
    				else {
    					rawData = splitedCommand[1].getBytes();
    				}
    				 

        			iv = Encryptor.generateIv(Constants.ivLength);
        			chksumIV = Checksum.computeChecksum(iv.getIV()); 
        			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
        			byte[] encryptedImg = Encryptor.encryptImage(rawData, generatedXorBytes);
        			byte[] shuffledEncryptedImg = Shuffle.shuffleImgBytes(encryptedImg, iv);
    				
    				BufferedImage encodedImage = DisplayEncoder.encodeBytes(shuffledEncryptedImg, iv.getIV(),  chksumIV);
    				ImageIO.write(encodedImage, "png", new File(splitedCommand[2]));	
    				System.out.println("Encoded data was written to "+ splitedCommand[2]);
    			}
    			else if(splitedCommand[0].equals("decode") || splitedCommand[0].equals("decodestring")) {
        			try {inputFile = new File(splitedCommand[1]);}	
        			catch(Exception NullPointerException){
        				System.out.println("Entered input filepath doesn't exist.\n");
        				continue;
        			}
        			
    				RotatedImageSampler sampler = DisplayDecoder.decodeFilePC(inputFile);
    				iv = new IvParameterSpec(sampler.getIV1());
    				chksumIV = Checksum.computeChecksum(iv.getIV()); 
    				if(chksumIV[0] != sampler.getIV1Checksum()[0]) {
        				iv = new IvParameterSpec(sampler.getIV2());
        				chksumIV = Checksum.computeChecksum(iv.getIV()); 
        				if(chksumIV[0] != sampler.getIV2Checksum()[0]) {
        					System.out.println("error! both iv checksum are wrong. exiting...");
        				}
    				}
    				byte[] unShuffledEncryptedImg = Deshuffle.getDeshuffledBytes(sampler.getDecodedData(), iv);
    				byte[] decryptedBytes = Decryptor.decryptImage(unShuffledEncryptedImg, skeySpec, iv);
    				
    				if(splitedCommand[0].equals("decode")){
            			BufferedImage decodedImage = convertToImageUsingGetRGB(decryptedBytes);
        				ImageIO.write(decodedImage, "png", new File(splitedCommand[2]));
        				System.out.println("Decoded image was written to "+ splitedCommand[2]);
    				}
    				else {
            			String decodedImageString = new String(decryptedBytes);
        				System.out.println("Decoded string is: "+ decodedImageString+"\n");
    				}
    			}
    			else if(splitedCommand[0].equals("exit")){
    				continueProgram = false;
    				System.out.println("Exiting..\n");
    				break;
    			}
    			else {
    	    		System.out.println("Usage: 'Encode [filepath] [target encoded filepath]'\n"
    	    				+ "Usage: 'EncodeString [String] [target encoded filepath]'\n"
    	    				+ "Usage: 'Decode [encoded filepath]'\n"
    	    				+ "Usage: 'DecodeString [encoded filepath]'\n"
    	    				+ "Usage: 'Exit' to stop execution.");
    			}
    		}
	    }			    
	    scanner.close();
	}
	
    private static BufferedImage convertToImageUsingGetRGB(byte[] imageData) {

		int index;
		byte[] dims = Arrays.copyOfRange(imageData, 0, 4);
		int width = signedShortToUnsignedInt(dims, 0, 2);
		int height = signedShortToUnsignedInt(dims, 2, 2);	
		byte[] chksumDims = Checksum.computeChecksum(dims); 
		if(chksumDims[0] != imageData[IMAGE_DIMS_ENCODING_LENGTH]) {
			dims = Arrays.copyOfRange(imageData, MAX_ENCODED_LENGTH_BYTES + CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH,
					MAX_ENCODED_LENGTH_BYTES + 2*(CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH) - CHECKSUM_LENGTH);
			width = signedShortToUnsignedInt(dims, 0, 2);
			height = signedShortToUnsignedInt(dims, 2, 2);
			if(chksumDims[0] != imageData[CHECKSUM_LENGTH]) {
				System.out.println("error! both dimensions checksum are wrong. exiting...");
				System.exit(-1);
			}
		}
		
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int channels = 4;
        int ARGB; 
        ByteBuffer wrapped;
        byte[] pixelData = Arrays.copyOfRange(imageData, CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH,
        		MAX_ENCODED_LENGTH_BYTES+CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH);
        
        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
        	  index = (row*width + col)*channels;
        	  wrapped = ByteBuffer.wrap(pixelData, index, channels);
        	  ARGB = wrapped.getInt();
        	  image.setRGB(col, row, ARGB);
           }
        }

        return image;
     }
    
	private static int signedShortToUnsignedInt(byte[] bytes, int start, int length) {
		short signedShort = ByteBuffer.wrap(bytes, start, length).getShort();
		return Short.toUnsignedInt(signedShort);
	}
}

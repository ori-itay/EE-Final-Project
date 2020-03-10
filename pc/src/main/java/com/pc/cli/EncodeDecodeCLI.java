package com.pc.cli;

import com.pc.checksum.Checksum;
import com.pc.FlowUtils;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

import static com.pc.configuration.Constants.*;



public class EncodeDecodeCLI {
	
	public static void main(String... args) throws Exception {
	    Scanner scanner = new Scanner(System.in);  // Create a Scanner object
	    File inputFile;
	    IvParameterSpec iv;
	    byte[] checksumIV;
		BufferedImage image;

	    while(true) {
			System.out.println("Enter Decode/Encode command:");
	    	String userCommand = scanner.nextLine().toLowerCase();  // Read user input

    		String[] splitedCommand = userCommand.split("\\s+");
    		if(splitedCommand.length < 2 && !splitedCommand[0].equals("exit")) {
	    		System.out.println("Usage: 'Encode [input image filepath] [output encoded image filepath]'\n"
	    				+ "Usage: 'Decode [encoded input image filepath] [output decoded image filepath]'\n"
	    				+ "Usage: 'Exit' to stop execution.");
    			continue;
    		}
    		else {  
    			/* constant key */
    			byte[] const_key = new byte[] {100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115};
    			SecretKeySpec skeySpec = new SecretKeySpec(const_key, Parameters.encryptionAlgorithm);
    			
    			if(splitedCommand[0].equals("encode")){
    				byte[] rawData;
    				
	       			try {
	       				inputFile = new File(splitedCommand[1]);
						image = ImageIO.read(inputFile);
	       			}
        			catch(Exception ex){
        				System.out.println("Entered input filepath has error.\n");
        				continue;
        			}

	       			rawData = FlowUtils.convertToBytesUsingGetRGB(image) ;
        			iv = Encryptor.generateIv(Parameters.ivLength);
					checksumIV = Checksum.computeChecksum(iv.getIV());
        			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
        			byte[] encryptedImg = Encryptor.encryptImage(rawData, generatedXorBytes);
        			byte[] shuffledEncryptedImg = Shuffle.shuffleImgPixels(encryptedImg, iv);
    				
    				BufferedImage encodedImage = DisplayEncoder.encodeBytes(shuffledEncryptedImg, iv.getIV(),  checksumIV);
    				ImageIO.write(encodedImage, "png", new File(splitedCommand[2]));	
    				System.out.println("Encoded data was written to "+ splitedCommand[2]);
    			}
    			else if(splitedCommand[0].equals("decode")) {
					RotatedImageSampler sampler;
        			try {
						sampler = DisplayDecoder.decodeFilePC(splitedCommand[1]);
        			}
        			catch(Exception NullPointerException){
						System.out.println("Entered input filepath has error.\n");
        				continue;
        			}
    				iv = new IvParameterSpec(sampler.getIV1());
					checksumIV = Checksum.computeChecksum(iv.getIV());
    				if(checksumIV[0] != sampler.getIV1Checksum()[0]) {
        				iv = new IvParameterSpec(sampler.getIV2());
						checksumIV = Checksum.computeChecksum(iv.getIV());
        				if(checksumIV[0] != sampler.getIV2Checksum()[0]) {
        					System.out.println("error! both iv checksum are wrong. exiting...");
        					System.exit(-1);
        				}
    				}
    				byte[] unShuffledEncryptedImg = Deshuffle.getDeshuffledBytes(sampler.getDecodedData(), iv);
    				byte[] decryptedBytes = Decryptor.decryptImage(unShuffledEncryptedImg, skeySpec, iv);

					BufferedImage decodedImage = convertToImageUsingGetRGB(decryptedBytes);
					ImageIO.write(decodedImage, "png", new File(splitedCommand[2]));
					System.out.println("Decoded image was written to "+ splitedCommand[2]);

    			}
    			else if(splitedCommand[0].equals("exit")){
    				System.out.println("Exiting..\n");
    				break;
    			}
    			else {
					System.out.println("Usage: 'Encode [input image filepath] [output encoded image filepath]'\n"
							+ "Usage: 'Decode [encoded input image filepath] [output decoded image filepath]'\n"
							+ "Usage: 'Exit' to stop execution.");
    			}
    		}
	    }			    
	    scanner.close();
	}
	
    private static BufferedImage convertToImageUsingGetRGB(byte[] imageData) {

		int index;
		
		byte[] dims = Arrays.copyOfRange(imageData, 0, IMAGE_DIMS_ENCODING_LENGTH);
		int width = signedShortToUnsignedInt(dims, 0, IMAGE_DIMENSION_ENCODING_LENGTH);
		int height = signedShortToUnsignedInt(dims, IMAGE_DIMENSION_ENCODING_LENGTH, IMAGE_DIMENSION_ENCODING_LENGTH);	
		byte[] checksumDims = Checksum.computeChecksum(dims);
		
		if(checksumDims[0] != imageData[IMAGE_DIMS_ENCODING_LENGTH]) {
			dims = Arrays.copyOfRange(imageData, imageData.length - (IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH),
					imageData.length - CHECKSUM_LENGTH);
			width = signedShortToUnsignedInt(dims, 0, 2);
			height = signedShortToUnsignedInt(dims, 2, 2);
			checksumDims = Checksum.computeChecksum(dims);
			if(checksumDims[0] != imageData[imageData.length - 1]) {
				System.out.println("error! both dimensions checksum are wrong. exiting...");
				System.exit(-1);
			}
		}
		int encodedLengthBytes = width*height*CHANNELS;
		
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int RGB;
        byte[] pixelData = Arrays.copyOfRange(imageData, CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH,
        		encodedLengthBytes+CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH);
        
        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
			  index = (row*width + col)*CHANNELS;
			  RGB = 0xFF000000 | pixelData[index]<<16 | pixelData[index+1]<<8 | pixelData[index+2];
			  image.setRGB(col, row, RGB);
           }
        }

        return image;
     }
    
	private static int signedShortToUnsignedInt(byte[] bytes, int start, int length) {
		short signedShort = ByteBuffer.wrap(bytes, start, length).getShort();
		return Short.toUnsignedInt(signedShort);
	}
}
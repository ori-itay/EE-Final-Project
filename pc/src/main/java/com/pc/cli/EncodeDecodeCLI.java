package com.pc.cli;

import com.pc.Flow;
import com.pc.checksum.Checksum;
import com.pc.FlowUtils;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.decryptor.Decryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.deshuffle.Deshuffle;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

import static com.pc.configuration.Constants.*;



public class EncodeDecodeCLI {


	/**
	 * CLI for "manual" encoding of an image file
	 */
	public static void main(String... args) throws Exception {
	    Scanner scanner = new Scanner(System.in);  // Create a Scanner object

	    while(true) {
			System.out.println("Enter command:");
	    	String userCommand = scanner.nextLine();  // Read user input

    		String[] splitedCommand = userCommand.split("\\s+");
    		if(splitedCommand.length < 2 && !splitedCommand[0].toLowerCase().equals("exit")) {
				System.out.println("Usage: 'Encode [input image filepath] [output encoded image filepath] [optional - colorLevels]'\n"
	    				+ "Usage: 'Exit' to stop execution.");
    			continue;
    		}
    		else {  
    			/* constant key */
    			byte[] const_key = new byte[] {100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115};
    			
    			if(splitedCommand[0].toLowerCase().equals("encode")){
					BufferedImage encodedImage = executeEncodingProccess(splitedCommand[1], const_key);
    				ImageIO.write(encodedImage, "png", new File(splitedCommand[2]));
					System.out.println((Constants.MODULES_IN_ENCODED_IMAGE_DIM - Parameters.modulesInMargin*2) +
							" modules in dimension (without margins).");

					Flow.gamma = 0.75;
					BufferedImage withGamma = executeEncodingProccess(splitedCommand[1], const_key);
					ImageIO.write(withGamma, "png", new File("gamma_"+splitedCommand[2]));
    			}
    			else if(splitedCommand[0].toLowerCase().equals("exit")){
    				System.out.println("Exiting..\n");
    				break;
    			}
    			else {
					System.out.println("Usage: 'Encode [input image filepath] [output encoded image filepath] [optional - colorLevels]'\n"
							+ "Usage: 'Exit' to stop execution.");
    			}
				if(splitedCommand.length == 4){
					Parameters.encodingColorLevels = Integer.parseInt(splitedCommand[3]);
				}
    		}
	    }			    
	    scanner.close();
	}



	/**
	 * Execute all encoding process for an image
	 * @param filepath - Path for the image file to be encoded
	 * @param const_key - The encryption key
	 * @return The encoded image
	 */
	public static BufferedImage executeEncodingProccess(String filepath, byte[] const_key) throws Exception {
		BufferedImage image;
		try {
			File inputFile = new File(filepath);
			image = ImageIO.read(inputFile);
		}
		catch(Exception ex){
			System.out.println("Entered input filepath has error.\n");
			return null;
		}
		byte[] dimsArr = FlowUtils.getDimensionsArray(image);
		byte[] rawData = FlowUtils.convertToBytesUsingGetRGB(image) ;
		IvParameterSpec iv = new IvParameterSpec(new byte[] {1,2,3,4,5,6,7,8,9,10,11,12});//Encryptor.generateIv(Parameters.ivLength);
		byte[] checksumIV = Checksum.computeChecksum(iv.getIV());
		SecretKeySpec skeySpec = new SecretKeySpec(const_key, Parameters.encryptionAlgorithm);
		byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
		byte[] encryptedImg = Encryptor.encryptImage(rawData, generatedXorBytes);
		byte[] shuffledEncryptedImg = Shuffle.shuffleImgPixels(encryptedImg, iv);
		return DisplayEncoder.encodeBytes(shuffledEncryptedImg, dimsArr, iv.getIV(),  checksumIV);
        //return DisplayEncoder.encodeBytes(rawData, dimsArr, iv.getIV(),  checksumIV);
	}

	/**
	 * Convert byte array to a BufferedImage
	 * @param imageData - The byte array
	 * @return The generated image
	 */
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

	/**
	 * Convert byte array to unsigned Int
	 * @param bytes - The byte array
	 * @param  start - Stat index in array
	 * @param  length - number of bytes to be converted
	 * @return The resulted integer
	 */
	private static int signedShortToUnsignedInt(byte[] bytes, int start, int length) {
		short signedShort = ByteBuffer.wrap(bytes, start, length).getShort();
		return Short.toUnsignedInt(signedShort);
	}
}
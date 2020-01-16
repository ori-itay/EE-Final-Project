package com.pc.cli;
import static com.pc.configuration.Constants.DIMENSIONS_ENCODING_BYTE_LEN;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Scanner;

import javax.imageio.ImageIO;

import com.pc.FlowUtils;
import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encoderDecoder.RotatedImageSampler;



public class EncodeDecodeCLI {
	
	public static void main(String... args) throws Exception {
		
		boolean continueProgram = true;
	    Scanner scanner = new Scanner(System.in);  // Create a Scanner object
	    File inputFile;
	    BufferedImage encodedImage;
	    
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
    			switch (splitedCommand[0]) {
    			case "encode":
        			try {inputFile = new File(splitedCommand[1]);}	
        			catch(Exception NullPointerException){
        				System.out.println("Entered input filepath doesn't exist.\n");
        				continue;
        			}
    				//fileContent = new String(Files.readAllBytes(inputFile.toPath()));
        			BufferedImage image = ImageIO.read(inputFile);
        			byte[] pixelRGB_Array = FlowUtils.convertToBytesUsingGetRGB(image) ;
    				encodedImage = DisplayEncoder.encodeBytes(pixelRGB_Array);
    				ImageIO.write(encodedImage, "png", new File(splitedCommand[2]));	
    				System.out.println("Encoded image was written to "+ splitedCommand[2]);
    				break;
    			case "encodestring":
    				encodedImage = DisplayEncoder.encodeBytes(splitedCommand[1].getBytes());
    				ImageIO.write(encodedImage, "png", new File(splitedCommand[2]));	
    				System.out.println("Encoded string was written to "+ splitedCommand[2]);
    				break;
    			case "decode":
        			try {inputFile = new File(splitedCommand[1]);}	
        			catch(Exception NullPointerException){
        				System.out.println("Entered input filepath doesn't exist.\n");
        				continue;
        			}
        			RotatedImageSampler sampler = DisplayDecoder.decodeFilePC(inputFile);
        			BufferedImage decodedImage = convertToImageUsingGetRGB(sampler.getDecodedData(), sampler.getImageWidth(), sampler.getImageHeight());
    				ImageIO.write(decodedImage, "png", new File(splitedCommand[2]));
    				System.out.println("Decoded image was written to "+ splitedCommand[2]);
    				break;
    			case "decodestring":
        			try {inputFile = new File(splitedCommand[1]);}	
        			catch(Exception NullPointerException){
        				System.out.println("Entered input filepath doesn't exist.\n");
        				continue;
        			}
        			String decodedImageString = new String(DisplayDecoder.decodeFilePC(inputFile).getDecodedData());
    				System.out.println("Decoded string is: "+ decodedImageString+"\n");
    				break;
    				
    			case "exit":
    				continueProgram = false;
    				System.out.println("Exiting..\n");
    				break;
    			default:
    		    		System.out.println("Usage: 'Encode [filepath] [target encoded filepath]'\n"
    		    				+ "Usage: 'Decode [encoded filepath]'\n"
    		    				+ "Usage: 'Exit' to stop execution.");
    			}
    				
    		}
	    }	    
	    scanner.close();
	}
	
    private static BufferedImage convertToImageUsingGetRGB(byte[] imageData, int width, int height) {

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

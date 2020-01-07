package com.pc.cli;

import static com.pc.configuration.Constants.MODULES_IN_ENCODED_IMAGE_DIM;
import static com.pc.configuration.Constants.PIXELS_IN_MODULE;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Scanner;
import javax.imageio.ImageIO;

import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encoderDecoder.RotatedImageSampler;


public class EncodeDecodeCLI {
	
	public static void main(String... args) throws Exception {
		
		boolean continueProgram = true;
	    Scanner scanner = new Scanner(System.in);  // Create a Scanner object
	    String fileContent;
	    File inputFile;
	    BufferedImage encodedImage;
	    byte[] decodedImageData;
	    
	    System.out.println("Enter Decode/Encode command:");
	    
	    while(continueProgram) {
	    	String userCommand = scanner.nextLine().toLowerCase();  // Read user input

    		String[] splitedCommand = userCommand.split("\\s+");
    		if(splitedCommand.length > 3 || splitedCommand.length < 2 ||
    				(splitedCommand.length == 2 && (splitedCommand[0].equals("encode") || splitedCommand[0].equals("decode")) ) ) {
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
    				fileContent = new String(Files.readAllBytes(inputFile.toPath()));
    				encodedImage = DisplayEncoder.encodeBytes(fileContent.getBytes());
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
        			decodedImageData = DisplayDecoder.decodeFilePC(inputFile).getDecodedData();
    				System.out.println("Decoded string is: "+ new String(decodedImageData)+"\n");
    				/*BufferedImage decodedImage = new BufferedImage(MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE,
    						MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE, BufferedImage.TYPE_INT_ARGB);
    		        for (int row=0; row < imageSampler.getPixelMatrix()[0].length ; row++) {
    		        	 for (int col=0; col < imageSampler.getPixelMatrix().length ; col++) {
    		        		 decodedImage.setRGB(col, row, imageSampler.getPixelMatrix()[row][col]);
    		        	 }
    		         }
    				ImageIO.write(decodedImage, "png", new File(splitedCommand[2]));*/
    			    BufferedWriter writer = new BufferedWriter(new FileWriter(splitedCommand[2]));
    			    writer.write(new String(decodedImageData));
    			    writer.close();
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

}

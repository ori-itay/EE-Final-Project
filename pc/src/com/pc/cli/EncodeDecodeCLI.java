package com.pc.cli;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;
import javax.imageio.ImageIO;

import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;


public class EncodeDecodeCLI {
	
	public static void main(String... args) throws Exception {
		
		boolean continueProgram = true;
	    Scanner scanner = new Scanner(System.in);  // Create a Scanner object
	    String fileContent;
	    File inputFile;
	    
	    System.out.println("Enter Decode/Encode command:");
	    
	    while(continueProgram) {
	    	String userCommand = scanner.nextLine().toLowerCase();  // Read user input

    		String[] splitedCommand = userCommand.split("\\s+");
    		if(splitedCommand.length > 3 || (splitedCommand.length == 2 && splitedCommand[0].equals("encode"))
    				|| (splitedCommand.length == 3 && splitedCommand[0].equals("decode")) ) {
	    		System.out.println("Usage: 'Encode [filepath] [target encoded filepath]'\n"
	    				+ "Usage: 'Decode [encoded filepath]'\n"
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
    				BufferedImage encodedImage = DisplayEncoder.encodeBytes(fileContent);
    				ImageIO.write(encodedImage, "png", new File(splitedCommand[2]));	
    				System.out.println("Encoded image was written to "+ splitedCommand[2]);
    				break;
    			case "decode":
        			try {inputFile = new File(splitedCommand[1]);}	
        			catch(Exception NullPointerException){
        				System.out.println("Entered input filepath doesn't exist.\n");
        				continue;
        			}
    				String decodedString = new String(DisplayDecoder.decodeImage(inputFile).decodedData);
    				System.out.println("Decoded string is: "+decodedString+"\n");
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

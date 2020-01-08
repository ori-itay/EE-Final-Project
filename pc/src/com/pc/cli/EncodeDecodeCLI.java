package com.pc.cli;
import static com.pc.configuration.Constants.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Scanner;
import javax.imageio.ImageIO;

import com.pc.encoderDecoder.DisplayDecoder;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encoderDecoder.StdImageSampler;
import com.pc.Flow;



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
        			byte[] pixelRGB_Array = convertToBytesUsingGetRGB(image) ;
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
        			BufferedImage decodedImage = Flow.convertToImageUsingGetRGB(sampler.getDecodedData(), sampler.getImageWidth(), sampler.getImageHeight());
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
	
    private static byte[] convertToBytesUsingGetRGB(BufferedImage image) {

        assert(image.getWidth() < Short.MAX_VALUE && image.getHeight() < Short.MAX_VALUE);
        
        short width = (short) image.getWidth();
        short height = (short) image.getHeight();
        
        int channels = 4, ARGB, index;

        if(DIMENSIONS_ENCODING_BYTE_LEN + height*width*channels > MAX_ENCODED_LENGTH/8) {
        	System.out.println("file too large\n");
        	System.exit(1); 
        }
        
        byte[] imageData = new byte[MAX_ENCODED_LENGTH/8];
        
        
        imageData[0] = (byte) (width >>> 8); imageData[1] = (byte) width;
        imageData[2] = (byte) (height >>> 8); imageData[3] = (byte) height;
        

        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
        	   ARGB = image.getRGB(col, row);
        	   index = (row*width + col)*channels;
        	   
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index] = (byte) (ARGB >>> 24); //alpha
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index + 1] = (byte) (ARGB >>> 16);//red
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index + 2] = (byte) (ARGB >>> 8);//green
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index + 3] = (byte) ARGB;//blue
           }
        }
        //pad with '0'
        for(int i = DIMENSIONS_ENCODING_BYTE_LEN + height*width*channels; i < MAX_ENCODED_LENGTH/8; i++) {
        	imageData[i] = (byte) 0;
        }
                
        return imageData;
     }

}

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
        			BufferedImage decodedImage = convertToImageUsingGetRGB(sampler.getDecodedData());
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
        
        int channels = 4, ARGB, currChannel, index;
        ByteBuffer wrapper;

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
        	   
        	   Color c = new Color(ARGB, true);
        	   
        	   byte alpha = (byte) c.getAlpha();
        	   byte red = (byte) c.getRed();
        	   byte green = (byte) c.getGreen();
        	   byte blue = (byte) c.getBlue();
        	   
        	   index = (row*width + col)*channels;
        	   
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index] = (byte) c.getAlpha();
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index + 1] = (byte) c.getRed();
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index + 2] = (byte) c.getGreen();
        	   imageData[DIMENSIONS_ENCODING_BYTE_LEN + index + 3] = (byte) c.getBlue();
        	   
 //       	   for(int channel = 0; channel<channels; channel++) {
//        		   currChannel = ARGB >>> (BITS_IN_BYTE*(3-channel));
//        		   imageData[DIMENSIONS_ENCODING_BYTE_LEN + row*width + col + channel] = (byte) currChannel;
        		   
        		   
 //       	   }
        
//        	   wrapper = ByteBuffer.wrap(imageData, DIMENSIONS_ENCODING_BYTE_LEN + row*width + col, channels);
//        	   wrapper.putInt(ARGB);
        	   //imageData[DIMENSIONS_ENCODING_BYTE_LEN + row*width + col] = (byte) (image.getRGB(col, row));
           }
        }
        //pad with '0'
        for(int i = DIMENSIONS_ENCODING_BYTE_LEN + height*width*channels; i < MAX_ENCODED_LENGTH/8; i++) {
        	imageData[i] = (byte) 0;
        }
                
        return imageData;
     }
    
    private static BufferedImage convertToImageUsingGetRGB(byte[] imageData) {

		int width = signedShortToUnsignedInt(imageData, 0, 2);
		int height = signedShortToUnsignedInt(imageData, 2, 2);
		int index;
    	
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int channels = 4;
        int ARGB; 
        ByteBuffer wrapped;
        
        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
        	  index = (row*width + col)*channels;
        	  //ARGB = signedShortToUnsignedInt(imageData, DIMENSIONS_ENCODING_BYTE_LEN + row*width + col, channels); 
        	  wrapped = ByteBuffer.wrap(imageData, DIMENSIONS_ENCODING_BYTE_LEN + index, channels);
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

package com.pc;

import static com.pc.configuration.Constants.DIMENSIONS_ENCODING_BYTE_LEN;
import static com.pc.configuration.Constants.MAX_ENCODED_LENGTH;

import java.awt.image.BufferedImage;

import com.pc.configuration.Constants;

public class FlowUtils {
    public static byte[] convertToBytesUsingGetRGB(BufferedImage image) {

        assert(image.getWidth() < Short.MAX_VALUE && image.getHeight() < Short.MAX_VALUE);
        
        short width = (short) image.getWidth();
        short height = (short) image.getHeight();
        
        int channels = 4, ARGB, index;

        if(DIMENSIONS_ENCODING_BYTE_LEN + height*width*channels > Constants.MAX_ENCODED_LENGTH_BYTES) {
        	System.out.println("file too large\n");
        	System.exit(1); 
        }
        
        byte[] imageData = new byte[Constants.MAX_ENCODED_LENGTH_BYTES];
        
        
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

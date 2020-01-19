package com.pc;

import static com.pc.configuration.Constants.CHECKSUM_LENGTH;
import static com.pc.configuration.Constants.IMAGE_DIMS_ENCODING_LENGTH;
import static com.pc.configuration.Constants.MAX_ENCODED_LENGTH_BYTES;

import java.awt.image.BufferedImage;

import com.checksum.Checksum;

public class FlowUtils {
    public static byte[] convertToBytesUsingGetRGB(BufferedImage image) {

        assert(image.getWidth() < Short.MAX_VALUE && image.getHeight() < Short.MAX_VALUE);
        
        short width = (short) image.getWidth();
        short height = (short) image.getHeight();
        byte[] dimsChecksum;
        
        int channels = 4, ARGB, index;

        if(height*width*channels > MAX_ENCODED_LENGTH_BYTES) {
        	System.out.println("file too large\n");
        	System.out.println("max input image size is:" + MAX_ENCODED_LENGTH_BYTES+"\n");
        	System.exit(1); 
        }
        
        byte[] imageData = new byte[MAX_ENCODED_LENGTH_BYTES];
        byte[] dimsArr = new byte[IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH];
        
        //create image dims+checksum byte array
        dimsArr[0] = (byte) (width >>> 8); dimsArr[1] = (byte) width;
        dimsArr[2] = (byte) (height >>> 8); dimsArr[3] = (byte) height;
        
        dimsChecksum = Checksum.computeChecksum(new byte[] {dimsArr[0],dimsArr[1],dimsArr[2],dimsArr[3]});
        dimsArr[4] = dimsChecksum[0];
        

        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
        	   ARGB = image.getRGB(col, row);
        	   index = (row*width + col)*channels;
        	   
        	   imageData[index] = (byte) (ARGB >>> 24); //alpha
        	   imageData[index + 1] = (byte) (ARGB >>> 16);//red
        	   imageData[index + 2] = (byte) (ARGB >>> 8);//green
        	   imageData[index + 3] = (byte) ARGB;//blue
           }
        }
        //pad with '0'
        for(int i = height*width*channels; i < MAX_ENCODED_LENGTH_BYTES; i++) {
        	imageData[i] = (byte) 0;
        }
        
        return concatenateByteArrays(dimsArr, imageData, dimsArr);
     }
    
    private static byte[] concatenateByteArrays(byte[] dimsArr, byte[] imageData, byte[] dimsArr2) {
        int aLen = dimsArr.length;
        int bLen = imageData.length;
        int cLen = dimsArr2.length;

        byte[] d = new byte[aLen + bLen + cLen];
        System.arraycopy(dimsArr, 0, d, 0, aLen);
        System.arraycopy(imageData, 0, d, aLen, bLen);
        System.arraycopy(dimsArr2, 0, d, aLen+bLen, cLen);

        return d;
    }
    
}

package com.pc;

import static com.pc.configuration.Parameters.*;

import java.awt.image.BufferedImage;

import com.checksum.Checksum;

public class FlowUtils {
    public static byte[] convertToBytesUsingGetRGB(BufferedImage image) {

        assert(image.getWidth() < Short.MAX_VALUE && image.getHeight() < Short.MAX_VALUE);
        
        short width = (short) image.getWidth();
        short height = (short) image.getHeight();
        byte[] dimsChecksum;
        
        int channels = 4, ARGB, index;

        if(height > MAX_IMAGE_DIMENSION_SIZE || width > MAX_IMAGE_DIMENSION_SIZE) {
        	System.out.println("file too large\n");
        	System.out.println("max input image dimension is:" + MAX_IMAGE_DIMENSION_SIZE+"\n");
        	System.exit(1); 
        }
        
        byte[] imageData = new byte[width+height*channels];
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
        
        //pad to natural number of modules  
        int padBytesNum = 0;
        int totalDataBits = BITS_IN_BYTE*(imageData.length + 2*dimsArr.length);
        while ((totalDataBits + padBytesNum*BITS_IN_BYTE)%ENCODING_BIT_GROUP_SIZE != 0) {
        	padBytesNum++;
        }
        byte[] pad = new byte[padBytesNum];
        
        return concatenateByteArrays(dimsArr, imageData, pad, dimsArr);
     }
    
    private static byte[] concatenateByteArrays(byte[] dimsArr, byte[] imageData, byte[] pad, byte[] dimsArr2) {
        int aLen = dimsArr.length;
        int bLen = imageData.length;
        int cLen = pad.length;
        int dLen = dimsArr2.length;

        byte[] res = new byte[aLen + bLen + cLen + dLen];
        System.arraycopy(dimsArr, 0, res, 0, aLen);
        System.arraycopy(imageData, 0, res, aLen, bLen);
        System.arraycopy(dimsArr2, 0, res, aLen+bLen, cLen);
        System.arraycopy(dimsArr2, 0, res, aLen+bLen+cLen, dLen);

        return res;
    }
    
}

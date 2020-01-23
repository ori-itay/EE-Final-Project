package com.pc;

import static com.pc.configuration.Constants.*;

import java.awt.image.BufferedImage;

import com.checksum.Checksum;
import com.pc.configuration.Parameters;

public class FlowUtils {
    public static byte[] convertToBytesUsingGetRGB(BufferedImage image) {
        
        short width = (short) image.getWidth();
        short height = (short) image.getHeight();
        byte[] dimsChecksum;
        
        int channels = 4, ARGB, index;

        if(height > MAX_IMAGE_DIMENSION_SIZE || width > MAX_IMAGE_DIMENSION_SIZE) {
        	System.out.println("file too large\n");
        	System.out.println("max input image dimension is:" + MAX_IMAGE_DIMENSION_SIZE+"\n");
        	System.exit(1); 
        }
        
        byte[] imageData = new byte[width*height*channels];
        byte[] dimsArr = new byte[IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH];
        

        int totalLengthToEncode = computeMinDimensionAndReturnMaxEncodedLength(imageData.length + 2*dimsArr.length);
        
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
        
        //padding
        int padLength = totalLengthToEncode - (imageData.length + 2*dimsArr.length);
        byte[] pad = new byte[padLength];
        
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
        System.arraycopy(pad, 0, res, aLen+bLen, cLen);
        System.arraycopy(dimsArr2, 0, res, aLen+bLen+cLen, dLen);

        return res;
    }
    
    
    
	private static int computeMinDimensionAndReturnMaxEncodedLength(int dataLength) {
		int totalNumOfBits = dataLength*BITS_IN_BYTE;
		int modulesForEncoding = Math.floorDiv(totalNumOfBits, ENCODING_BIT_GROUP_SIZE);
		int dim = (int) Math.ceil(Math.sqrt(modulesForEncoding)) + 2*(MODULES_IN_POS_DET_DIM+Parameters.modulesInMargin); // initial guess
		int maxEncodedLength;
		
		while((maxEncodedLength=computeMaxEncodedLength(dim)) > dataLength) 
			dim--;
		while((maxEncodedLength=computeMaxEncodedLength(dim)) < dataLength) 
			dim++;
		
		MODULES_IN_ENCODED_IMAGE_DIM = dim;
		return maxEncodedLength;
	}

	public static int computeMaxEncodedLength(int dim) {
		int maxBitsToEncode = ENCODING_BIT_GROUP_SIZE*(dim*dim 
				- 4*Parameters.modulesInMargin*(dim -Parameters.modulesInMargin)
				- MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS
				-2*BITS_IN_BYTE*(Parameters.ivLength+CHECKSUM_LENGTH));
		
		return Math.floorDiv(maxBitsToEncode, BITS_IN_BYTE);
	}
    
}

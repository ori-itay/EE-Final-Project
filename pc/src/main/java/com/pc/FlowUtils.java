package com.pc;

import com.pc.checksum.Checksum;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import java.awt.image.BufferedImage;

import static com.pc.configuration.Constants.*;

public class FlowUtils {
    public static byte[] convertToBytesUsingGetRGB(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ARGB, index;
        int imageDataLen = width*height*CHANNELS;

        int totalLengthToEncode = computeMinDimensionAndReturnMaxEncodedLength(imageDataLen);
        byte[] resultArray = new byte[totalLengthToEncode];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                ARGB = image.getRGB(col, row);
                index = (row*width + col)*CHANNELS;

                resultArray[index] = (byte) (ARGB >>> 16);//red
                resultArray[index + 1] = (byte) (ARGB >>> 8);//green
                resultArray[index + 2] = (byte) ARGB;//blue
            }
        }
        return resultArray;
    }



    private static int computeMinDimensionAndReturnMaxEncodedLength(int bytesDataLen) {
        int totalNumOfBits = bytesDataLen*BITS_IN_BYTE;
        int modulesForEncoding = Math.floorDiv(totalNumOfBits, ENCODING_BIT_GROUP_SIZE*CHANNELS);
        int dim = (int) Math.ceil(Math.sqrt(modulesForEncoding)) + 2*(MODULES_IN_POS_DET_DIM+Parameters.modulesInMargin); // initial guess
        int maxEncodedLength;

        while(computeMaxEncodedLength(dim, Parameters.modulesInMargin) > bytesDataLen)
            dim--;
        while((maxEncodedLength=computeMaxEncodedLength(dim, Parameters.modulesInMargin)) < bytesDataLen)
            dim++;

        MODULES_IN_ENCODED_IMAGE_DIM = dim;
        return maxEncodedLength;
    }

    public static int computeMaxEncodedLength(int dim, int effectiveModulesInMargin) {
        //metadata (i.e iv+checksum + dims+checksum) is encoded "three times" - once in each channel (RGB)
        int modulesForIVChecksum = 2*(int)(Math.ceil(BITS_IN_BYTE*CHECKSUM_LENGTH/(double)ENCODING_BIT_GROUP_SIZE));
        int modulesForIV = 2*(int)(Math.ceil(BITS_IN_BYTE*Parameters.ivLength/(double)ENCODING_BIT_GROUP_SIZE));
        int modulesForDims = 2*(int)(Math.ceil(BITS_IN_BYTE*(IMAGE_DIMS_ENCODING_LENGTH+CHECKSUM_LENGTH)/(double)ENCODING_BIT_GROUP_SIZE));
        int modulesForMetadata = modulesForIVChecksum + modulesForIV + modulesForDims;

        int modulesForEncoding = (dim - 2*effectiveModulesInMargin)*(dim - 2*effectiveModulesInMargin)
                - MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS
                - MODULES_IN_ALIGNMENT_PATTERN_DIM*MODULES_IN_ALIGNMENT_PATTERN_DIM
                - modulesForMetadata -1; //-1 for bottom right module to be black for corner detection

        int maxBitsToEncode = CHANNELS*ENCODING_BIT_GROUP_SIZE*modulesForEncoding;
        return maxBitsToEncode/BITS_IN_BYTE;
    }

    public static byte[] getDimensionsArray(BufferedImage image){
        short width = (short) image.getWidth();
        short height = (short) image.getHeight();
        byte[] dimsArr = new byte[IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH];

        if(height > MAX_IMAGE_DIMENSION_SIZE || width > MAX_IMAGE_DIMENSION_SIZE) {
            System.out.println("file too large\n");
            System.out.println("max input image dimension is:" + MAX_IMAGE_DIMENSION_SIZE+"\n");
            System.exit(1);
        }

        dimsArr[0] = (byte) (width >>> 8);
        dimsArr[1] = (byte) width;
        dimsArr[2] = (byte) (height >>> 8);
        dimsArr[3] = (byte) height;

        byte[] dimsChecksum = Checksum.computeChecksum(new byte[] {dimsArr[0],dimsArr[1],dimsArr[2],dimsArr[3]});
        dimsArr[4] = dimsChecksum[0];

        return dimsArr;
    }


}

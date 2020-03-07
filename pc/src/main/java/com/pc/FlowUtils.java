package com.pc;

import com.pc.checksum.Checksum;
import com.pc.configuration.Parameters;
import java.awt.image.BufferedImage;

import static com.pc.configuration.Constants.*;

public class FlowUtils {
    public static byte[] convertToBytesUsingGetRGB(BufferedImage image) {

        short width = (short) image.getWidth();
        short height = (short) image.getHeight();
        byte[] dimsChecksum;

        int ARGB, index;

        if(height > MAX_IMAGE_DIMENSION_SIZE || width > MAX_IMAGE_DIMENSION_SIZE) {
            System.out.println("file too large\n");
            System.out.println("max input image dimension is:" + MAX_IMAGE_DIMENSION_SIZE+"\n");
            System.exit(1);
        }

        int imageDataLen = width*height*CHANNELS;
        int dimsAndChecksumLen = 2*(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH);

        int totalLengthToEncode = computeMinDimensionAndReturnMaxEncodedLength(imageDataLen + dimsAndChecksumLen);
        byte[] resultArray = new byte[totalLengthToEncode];

        //insert image dims+checksum into result array
        resultArray[0] = resultArray[totalLengthToEncode-5] = (byte) (width >>> 8);
        resultArray[1] = resultArray[totalLengthToEncode-4] =  (byte) width;
        resultArray[2] = resultArray[totalLengthToEncode-3] = (byte) (height >>> 8);
        resultArray[3] = resultArray[totalLengthToEncode-2] = (byte) height;

        dimsChecksum = Checksum.computeChecksum(new byte[] {resultArray[0],resultArray[1],resultArray[2],resultArray[3]});
        resultArray[4] = resultArray[totalLengthToEncode-1] = dimsChecksum[0];


        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                ARGB = image.getRGB(col, row);
                index = 5 + (row*width + col)*CHANNELS;

                resultArray[index] = (byte) (ARGB >>> 16);//red
                resultArray[index + 1] = (byte) (ARGB >>> 8);//green
                resultArray[index + 2] = (byte) ARGB;//blue
            }
        }
        return resultArray;
    }



    private static int computeMinDimensionAndReturnMaxEncodedLength(int dataLength) {
        int totalNumOfBits = dataLength*BITS_IN_BYTE;
        int modulesForEncoding = Math.floorDiv(totalNumOfBits, ENCODING_BIT_GROUP_SIZE);
        int dim = (int) Math.ceil(Math.sqrt(modulesForEncoding)) + 2*(MODULES_IN_POS_DET_DIM+Parameters.modulesInMargin); // initial guess
        int maxEncodedLength;

        while(computeMaxEncodedLength(dim) > dataLength)
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
                -2*CHANNELS*BITS_IN_BYTE*(Parameters.ivLength+CHECKSUM_LENGTH));
        //metadata (i.e iv+checksum) is encoded "three times" - once in each channel (RGB)

        return Math.floorDiv(CHANNELS*maxBitsToEncode, BITS_IN_BYTE);
    }

}

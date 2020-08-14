package com.pc;

import com.pc.checksum.Checksum;
import com.pc.configuration.Parameters;

import java.awt.image.BufferedImage;

import static com.pc.configuration.Constants.*;

/**
 * A utility class of @Flow
 */
public class FlowUtils {

    public static byte[] convertToBytesUsingGetRGB(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ARGB, index;
        int imageDataLen = width*height;

        int totalLengthToEncode = computeMinDimensionAndReturnMaxEncodedLength(imageDataLen);
        byte[] resultArray = new byte[CHANNELS * totalLengthToEncode];

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



    public static int computeMinDimensionAndReturnMaxEncodedLength(int bytesDataLen) {
        int modulesForIVChecksum =  2*(int)(( (BITS_IN_BYTE*CHECKSUM_LENGTH + ENCODING_BIT_GROUP_SIZE - 1)/(double)ENCODING_BIT_GROUP_SIZE));
        int modulesForIV = 2*(int)(( (BITS_IN_BYTE*Parameters.ivLength + ENCODING_BIT_GROUP_SIZE - 1)/(double)ENCODING_BIT_GROUP_SIZE));
        int modulesForDims = 2*(int)( ((BITS_IN_BYTE*(IMAGE_DIMS_ENCODING_LENGTH+CHECKSUM_LENGTH) + ENCODING_BIT_GROUP_SIZE - 1)
                /(double)ENCODING_BIT_GROUP_SIZE));
        int modulesForMetadata = modulesForIVChecksum + modulesForIV + modulesForDims;
        int modulesForAlignmentPattern = MODULES_IN_ALIGNMENT_PATTERN_DIM*MODULES_IN_ALIGNMENT_PATTERN_DIM;
        int modulesForImageData = (bytesDataLen*(BITS_IN_BYTE - Parameters.colorDiscardedBits) + ENCODING_BIT_GROUP_SIZE - 1)/(ENCODING_BIT_GROUP_SIZE); // ceil rounding "trick"
        int modulesForPosDet = MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS;
        int modulesForRightLowerCorner = 1;

        int totalNumOfModules = modulesForMetadata + modulesForPosDet + modulesForAlignmentPattern + modulesForRightLowerCorner + modulesForImageData;


        int dim = (int) Math.ceil(Math.sqrt(totalNumOfModules));
        int encodingModules = dim*dim - (modulesForMetadata + modulesForPosDet + modulesForAlignmentPattern + modulesForRightLowerCorner);
        int maxEncodedLength = (encodingModules*ENCODING_BIT_GROUP_SIZE)/(BITS_IN_BYTE - Parameters.colorDiscardedBits);

        //TODO: treat case of small dimension without alignment pattern
        MODULES_IN_ENCODED_IMAGE_DIM = dim + 2*Parameters.modulesInMargin;
        return maxEncodedLength;
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

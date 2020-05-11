package com.android.visualcrypto.configurationFetcher;

import com.pc.checksum.Checksum;
import com.pc.encoderDecoder.StdImageSampler;

import java.nio.ByteBuffer;

import static com.pc.configuration.Constants.IMAGE_DIMENSION_ENCODING_LENGTH;

public class DimensionsFetcher {


    private int width = 0;
    private int height = 0;

    /*
     * @pre format of imageBytes: width1 height1 checksum1 data data ..... width2 height2 checksum2
     */
    public DimensionsFetcher(StdImageSampler sampler){
        int width1 = getIntByteBuffer(sampler.getDims1(), 0, IMAGE_DIMENSION_ENCODING_LENGTH);
        int height1 = getIntByteBuffer(sampler.getDims1(),
                IMAGE_DIMENSION_ENCODING_LENGTH, IMAGE_DIMENSION_ENCODING_LENGTH);
//        byte[] dimensionsChecksum1 = ByteBuffer.wrap(imageBytes,
//                2*IMAGE_DIMENSION_ENCODING_LENGTH, CHECKSUM_LENGTH).array();
        //byte[] dimensionsChecksum1 = Arrays.copyOfRange(sampler.getDims1(), IMAGE_DIMS_ENCODING_LENGTH, IMAGE_DIMS_ENCODING_LENGTH+CHECKSUM_LENGTH);

        boolean validDimensions1 = Checksum.isValidChecksum(width1, height1, sampler.getDims1Checksum());

        if (validDimensions1){
            this.width = width1;
            this.height = height1;
            return;
        }

        //int startOfWidthIndex = imageBytes.length - (2*IMAGE_DIMENSION_ENCODING_LENGTH) - CHECKSUM_LENGTH;
        int width2 = getIntByteBuffer(sampler.getDims2(), 0, IMAGE_DIMENSION_ENCODING_LENGTH);
        int height2 = getIntByteBuffer(sampler.getDims2(),
                IMAGE_DIMENSION_ENCODING_LENGTH, IMAGE_DIMENSION_ENCODING_LENGTH);
        //byte[] dimensionsChecksum2 = ByteBuffer.wrap(imageBytes,
          //      startOfWidthIndex + 2*IMAGE_DIMENSION_ENCODING_LENGTH, CHECKSUM_LENGTH).array();

        boolean validDimensions2 = Checksum.isValidChecksum(width2, height2, sampler.getDims2Checksum());

        if (validDimensions2){
            this.width = width2;
            this.height = height2;
            return;
        }
    }

    public int getWidth(){
        return this.width;
    }

    public int getHeight(){
        return this.height;
    }


    private int getIntByteBuffer(byte[] bytes, int startIndex, int length) {
        short bytesShort = ByteBuffer.wrap(bytes, startIndex, length).getShort();
        return Short.toUnsignedInt(bytesShort);
    }
}

package com.android.visualcrypto.configurationFetcher;

import com.pc.checksum.Checksum;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encoderDecoder.StdImageSampler;

public class IvFetcher {

    public static byte[] getIV(StdImageSampler rotatedImageSampler) {
        byte[] IV1 = rotatedImageSampler.getIV1();
        byte[] IV1Checksum = rotatedImageSampler.getIV1Checksum();
        boolean validIV1 = Checksum.isValidChecksum(IV1Checksum, IV1);

        if (validIV1) {
            return IV1;
        }

        byte[] IV2 = rotatedImageSampler.getIV2();
        byte[] IV2Checksum = rotatedImageSampler.getIV2Checksum();
        boolean validIV2 = Checksum.isValidChecksum(IV2Checksum, IV2);

        if (validIV2) {
            return IV2;
        }

        return null;
    }

}

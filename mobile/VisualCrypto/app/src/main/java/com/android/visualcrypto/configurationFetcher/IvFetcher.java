package com.android.visualcrypto.configurationFetcher;

import android.util.Log;

import com.pc.checksum.Checksum;
import com.pc.encoderDecoder.RotatedImageSampler;
import com.pc.encoderDecoder.StdImageSampler;

import java.util.Arrays;

public class IvFetcher {

    public static byte[] getIV(StdImageSampler rotatedImageSampler) {
        byte[] IV1 = rotatedImageSampler.getIV1();
        Log.d("iv1", Arrays.toString(IV1));
        byte[] IV1Checksum = rotatedImageSampler.getIV1Checksum();
        boolean validIV1 = Checksum.isValidChecksum(IV1Checksum, IV1);

        if (validIV1) {
            return IV1;
        }

        byte[] IV2 = rotatedImageSampler.getIV2();
        Log.d("iv2", Arrays.toString(IV2));
        byte[] IV2Checksum = rotatedImageSampler.getIV2Checksum();
        boolean validIV2 = Checksum.isValidChecksum(IV2Checksum, IV2);

        if (validIV2) {
            return IV2;
        }

        return null;
    }

}

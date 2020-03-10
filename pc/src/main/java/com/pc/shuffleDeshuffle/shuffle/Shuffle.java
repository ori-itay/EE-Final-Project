package com.pc.shuffleDeshuffle.shuffle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import javax.crypto.spec.IvParameterSpec;

import com.pc.shuffleDeshuffle.ShuffleDeshuffle;

import static com.pc.configuration.Constants.*;

public class Shuffle {

	/*
	 * @Pre: iv.length >= 4 
	 */
	public static byte[] shuffleImgPixels(byte[] imgBytes, IvParameterSpec iv) {
		final int ivInt = ByteBuffer.wrap(iv.getIV(), 0, 4).getInt();
		final List<Integer> indexes = new ArrayList<>();
		ShuffleDeshuffle.generateShuffledIndexes(indexes, ivInt, imgBytes.length, CHANNELS);
		final byte[] shuffledImgBytes = new byte[imgBytes.length];
		System.arraycopy(imgBytes, 0, shuffledImgBytes, 0 , IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH);
		for (int i = 0; i < indexes.size(); i++) {
			shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i] = imgBytes[indexes.get(i)];
			shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i + 1] = imgBytes[indexes.get(i) + 1];
			shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i + 2] = imgBytes[indexes.get(i) + 2];
//			if (CHANNELS == 4) {
//				shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i + 3] = imgBytes[indexes.get(i) + 3];
//			}
		}
		int remainder = imgBytes.length % 3;
		switch (remainder) {
			case (0): {
				shuffledImgBytes[imgBytes.length - 3] = imgBytes[imgBytes.length - 3];
			} case (1): { // Notice no break in case 2
				shuffledImgBytes[imgBytes.length - 2] = imgBytes[imgBytes.length - 2];
			} case (2): {
				shuffledImgBytes[imgBytes.length - 1] = imgBytes[imgBytes.length - 1];
			}
		}
		return shuffledImgBytes;
	}
}

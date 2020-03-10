package com.pc.shuffleDeshuffle.deshuffle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.spec.IvParameterSpec;

import com.pc.shuffleDeshuffle.ShuffleDeshuffle;

import static com.pc.configuration.Constants.*;

public class Deshuffle {
	
	public static byte[] getDeshuffledBytes(byte[] shuffledImgBytes, IvParameterSpec iv) {
		final int ivInt = ByteBuffer.wrap(iv.getIV(), 0, 4).getInt();
		final List<Integer> indexes = new ArrayList<>();
		ShuffleDeshuffle.generateShuffledIndexes(indexes, ivInt, shuffledImgBytes.length, CHANNELS);
		final byte[] deshuffledImgBytes = new byte[shuffledImgBytes.length];

		System.arraycopy(shuffledImgBytes, 0, deshuffledImgBytes, 0 , IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH);
		for (int i = 0; i < indexes.size(); i++) {
			deshuffledImgBytes[indexes.get(i)] = shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i];
			deshuffledImgBytes[indexes.get(i) + 1] = shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i + 1];
			deshuffledImgBytes[indexes.get(i) + 2] = shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i + 2];
//			if (CHANNELS == 4) {
//				deshuffledImgBytes[indexes.get(i) + 3] = shuffledImgBytes[(IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH) + CHANNELS * i + 3];
//			}
		}
		int remainder = shuffledImgBytes.length % 3;
		switch (remainder) {
			case (0): {
				deshuffledImgBytes[shuffledImgBytes.length - 3] = shuffledImgBytes[shuffledImgBytes.length - 3];
			} case (1): { // Notice no break in case 2
				deshuffledImgBytes[shuffledImgBytes.length - 2] = shuffledImgBytes[shuffledImgBytes.length - 2];
			} case (2): {
				deshuffledImgBytes[shuffledImgBytes.length - 1] = shuffledImgBytes[shuffledImgBytes.length - 1];
			}
		}
		return deshuffledImgBytes;
	}
}

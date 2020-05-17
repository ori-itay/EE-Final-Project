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
		for (int i = 0; i < indexes.size(); i++) {
			shuffledImgBytes[CHANNELS * i] = imgBytes[indexes.get(i)];
			shuffledImgBytes[CHANNELS * i + 1] = imgBytes[indexes.get(i) + 1];
			shuffledImgBytes[CHANNELS * i + 2] = imgBytes[indexes.get(i) + 2];
		}
		return shuffledImgBytes;
	}
}

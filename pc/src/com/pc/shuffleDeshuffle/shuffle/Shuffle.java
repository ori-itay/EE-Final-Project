package com.pc.shuffleDeshuffle.shuffle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


import javax.crypto.spec.IvParameterSpec;

import com.pc.shuffleDeshuffle.ShuffleDeshuffle;

public class Shuffle {

	/*
	 * @Pre: iv.length >= 4 
	 */
	public static byte[] shuffleImgBytes(byte[] imgBytes, IvParameterSpec iv) {
		final int ivInt = ByteBuffer.wrap(iv.getIV(), 0, 4).getInt();
		final List<Integer> indexes = new ArrayList<>();
		ShuffleDeshuffle.generateShuffledIndexes(indexes, ivInt, imgBytes.length);
		final byte[] shuffledImgBytes = new byte[imgBytes.length];
		
		for (int i = 0; i < imgBytes.length; i++) {
			shuffledImgBytes[i] = imgBytes[indexes.get(i)];
		}
		return shuffledImgBytes;
	}
}

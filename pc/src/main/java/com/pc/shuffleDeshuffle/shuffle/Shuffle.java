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
	public static byte[] shuffleImgPixels(byte[] imgBytes, IvParameterSpec iv) {
		final int ivInt = ByteBuffer.wrap(iv.getIV(), 0, 4).getInt();
		final List<Integer> indexes = new ArrayList<>();
		ShuffleDeshuffle.generateShuffledIndexes(indexes, ivInt, imgBytes.length);
		final byte[] shuffledImgBytes = new byte[imgBytes.length];

		
		for (int i = 0; i < indexes.size(); i++) {
			shuffledImgBytes[4*i] = imgBytes[indexes.get(i)];
			shuffledImgBytes[4*i+1] = imgBytes[indexes.get(i)+1];
			shuffledImgBytes[4*i+2] = imgBytes[indexes.get(i)+2];
			shuffledImgBytes[4*i+3] = imgBytes[indexes.get(i)+3];
		}
		return shuffledImgBytes;
	}
}

package com.pc.shuffleDeshuffle.shuffle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


import javax.crypto.spec.IvParameterSpec;

import static com.pc.configuration.Constants.CHANNELS;
import com.pc.shuffleDeshuffle.ShuffleDeshuffle;

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
			shuffledImgBytes[CHANNELS*i] = imgBytes[indexes.get(i)];
			shuffledImgBytes[CHANNELS*i+1] = imgBytes[indexes.get(i)+1];
			shuffledImgBytes[CHANNELS*i+2] = imgBytes[indexes.get(i)+2];
			if (CHANNELS == 4) {
				shuffledImgBytes[CHANNELS*i+3] = imgBytes[indexes.get(i)+3];
			}
		}
		return shuffledImgBytes;
	}
}

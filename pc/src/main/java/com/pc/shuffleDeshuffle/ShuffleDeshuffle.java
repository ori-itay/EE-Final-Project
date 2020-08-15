package com.pc.shuffleDeshuffle;


import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ShuffleDeshuffle {

	/**
	 * Generates the index shuffle mapping in-place
	 * @param indexes - The original indexes
	 * @param ivInt - The IV as an integer
	 * @param length - The length of the data to be shuffled
	 * @param channels - The number of channels (ARGB or RGB)
	 */
	public static void generateShuffledIndexes(List<Integer> indexes, int ivInt, int length, int channels) {
		for (int i = 0; i < length - channels; i += channels) {
			indexes.add(i);
		}
		Collections.shuffle(indexes, new Random(ivInt));
	}
}

package com.pc.shuffleDeshuffle;

import com.pc.configuration.Constants;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ShuffleDeshuffle {

	public static void generateShuffledIndexes(List<Integer> indexes, int ivInt, int length, int channels) {
		for (int i = 0; i < length; i += channels) {
			indexes.add(i);
		}
		Collections.shuffle(indexes, new Random(ivInt));
	}
}

package com.pc.shuffleDeshuffle;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ShuffleDeshuffle {

	public static void generateShuffledIndexes(List<Integer> indexes, int ivInt, int length) {
		for (int i = 0; i < length; i += 4) {
			indexes.add(i);
		}
		Collections.shuffle(indexes, new Random(ivInt));
	}
}

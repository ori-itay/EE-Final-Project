package com.pc.shuffleDeshuffle;


import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.pc.configuration.Constants.CHECKSUM_LENGTH;
import static com.pc.configuration.Constants.IMAGE_DIMS_ENCODING_LENGTH;

public class ShuffleDeshuffle {

	public static void generateShuffledIndexes(List<Integer> indexes, int ivInt, int length, int channels) {
		for (int i = 0; i < length - channels; i += channels) {
			indexes.add(i);
		}
		Collections.shuffle(indexes, new Random(ivInt));
	}
}

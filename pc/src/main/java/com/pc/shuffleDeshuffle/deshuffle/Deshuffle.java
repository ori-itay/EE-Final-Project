package com.pc.shuffleDeshuffle.deshuffle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.spec.IvParameterSpec;

import com.pc.shuffleDeshuffle.ShuffleDeshuffle;

public class Deshuffle {
	
	public static byte[] getDeshuffledBytes(byte[] shuffledImgBytes, IvParameterSpec iv) {
		final int ivInt = ByteBuffer.wrap(iv.getIV(), 0, 4).getInt();
		final List<Integer> indexes = new ArrayList<>();
		ShuffleDeshuffle.generateShuffledIndexes(indexes, ivInt, shuffledImgBytes.length);
		final byte[] deshuffledImgBytes = new byte[shuffledImgBytes.length];
		
		for (int i = 0; i < indexes.size(); i++) {
			deshuffledImgBytes[indexes.get(i)] = shuffledImgBytes[4*i];
			deshuffledImgBytes[indexes.get(i)+1] = shuffledImgBytes[4*i+1];
			deshuffledImgBytes[indexes.get(i)+2] = shuffledImgBytes[4*i+2];
			deshuffledImgBytes[indexes.get(i)+3] = shuffledImgBytes[4*i+3];
		}
		return deshuffledImgBytes;
	}
}

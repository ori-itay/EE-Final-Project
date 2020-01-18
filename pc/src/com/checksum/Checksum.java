package com.checksum;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.pc.configuration.Constants;

public class Checksum {
	private static final int IV_CHECKSUM_INDEX = Constants.ivLength;
	private static final int DIMENSIONS_CHECKSUM_INDEX = IV_CHECKSUM_INDEX + 2 * Constants.IMAGE_DIMS_ENCODING_LENGTH;
	private static final int CHECKSUM_LENGTH = 1;
	
	public static byte[] computeChecksum(byte[] data) {
		byte[] checksum = new byte [CHECKSUM_LENGTH];
		
		int sum = 0;
		for (int i = 0; i < data.length; i++) {
			sum += data[i];
		}
		checksum[0] = (byte) sum;
		return checksum;
	}
	
	//TODO: test correctness
	public static char getIVChecksum(byte[] byteArr) {
		return ByteBuffer.allocate(1).put(byteArr[IV_CHECKSUM_INDEX]).getChar();
	}
	//TODO: test correctness
	public static char getDimensionChecksum(byte[] byteArr) {
		return ByteBuffer.allocate(1).put(byteArr[DIMENSIONS_CHECKSUM_INDEX]).getChar();
	}
	
	public static boolean isValidChecksum(byte[] checksum, byte[] data) {
		return Arrays.equals(computeChecksum(data), checksum);
	}
	
	public static boolean isValidChecksum(int width, int height, byte[] checksum) {
		byte[] widthAndHeightBytes =  ByteBuffer.allocate(8).putInt(width).putInt(height).array();
		
		return isValidChecksum(checksum, widthAndHeightBytes);
	}
}

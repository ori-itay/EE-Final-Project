package com.pc.checksum;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.pc.configuration.Constants;

public class Checksum {
	
	public static byte[] computeChecksum(byte[] data) {
		byte[] checksum = new byte [Constants.CHECKSUM_LENGTH];
		
		int sum = 0;
		for (int i = 0; i < data.length; i++) {
			sum += data[i];
		}
		checksum[0] = (byte) sum;
		return checksum;
	}
		
	public static boolean isValidChecksum(byte[] checksum, byte[] data) {
		return Arrays.equals(computeChecksum(data), checksum);
	}
	
	public static boolean isValidChecksum(int width, int height, byte[] checksum) {
		byte[] widthAndHeightBytes =  ByteBuffer.allocate(8).putInt(width).putInt(height).array();
		
		return isValidChecksum(checksum, widthAndHeightBytes);
	}
}

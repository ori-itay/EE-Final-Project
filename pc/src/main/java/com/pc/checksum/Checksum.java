package com.pc.checksum;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.pc.configuration.Constants;

/**
 * A class to verify the checksums in our schema
 */
public class Checksum {

	/**
	 * Computes the checksum from the data
	 * @param data - The data
	 * @return the checksum
	 */
	public static byte[] computeChecksum(byte[] data) {
		byte[] checksum = new byte [Constants.CHECKSUM_LENGTH];
		
		int sum = 0;
		for (int i = 0; i < data.length; i++) {
			sum += data[i];
		}
		checksum[0] = (byte) sum;
		return checksum;
	}

	/**
	 * Checks whether the data correspond to the given checksum
	 * @param checksum - The checksum
	 * @param data - The data
	 * @return whether the data correspond to the given checksum
	 */
	public static boolean isValidChecksum(byte[] checksum, byte[] data) {
		return Arrays.equals(computeChecksum(data), checksum);
	}

	/**
	 * Checks whether the dimensions data correspond to the given checksum
	 * @param width - The width
	 * @param height - The height
	 * @param checksum - The checksum
	 * @return whether the data correspond to the given checksum
	 */
	public static boolean isValidChecksum(int width, int height, byte[] checksum) {
		byte[] widthAndHeightBytes =  ByteBuffer.allocate(8).putInt(width).putInt(height).array();
		
		return isValidChecksum(checksum, widthAndHeightBytes);
	}
}

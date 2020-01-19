package com.pc.encoderDecoder;

import java.awt.image.BufferedImage;
import static com.pc.configuration.Constants.*;

public class StdImageSampler implements ImageSamplerInf{
	
	public BufferedImage getProccesedImage() {
		return proccesedImage;
	}

	public int[][] getPixelMatrix() {
		return pixelMatrix;
	}

	public int getModuleSize() {
		return moduleSize;
	}

	public byte[] getDecodedData() {
		return decodedData;
	}
	
	public byte[] getIV1() {
		return IV1;
	}

	public void setIV1(byte[] iV1) {
		IV1 = iV1;
	}

	public byte[] getIV1Checksum() {
		return IV1Checksum;
	}

	public void setIV1Checksum(byte[] iV1Checksum) {
		IV1Checksum = iV1Checksum;
	}

	public byte[] getIV2() {
		return IV2;
	}

	public void setIV2(byte[] iV2) {
		IV2 = iV2;
	}

	public byte[] getIV2Checksum() {
		return IV2Checksum;
	}

	public void setIV2Checksum(byte[] iV2Checksum) {
		IV2Checksum = iV2Checksum;
	}
	


	BufferedImage proccesedImage;
	int[][] pixelMatrix;
	int moduleSize = 0;
	byte[] decodedData;
	private byte[] IV1;
	private byte[] IV1Checksum;
	private byte[] IV2;
	private byte[] IV2Checksum;
	

	static void checkForColumnEnd(Position pos) {
		
		if(pos.colModule == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ) {
			//end of column in rows of top position detectors (except for the last top one)	
			if(pos.rowModule + 1 < MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM) {	
				pos.colModule = (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM);
				pos.rowModule++;
			}
			//end of column when in last row of top position detector	
			else if (pos.rowModule + 1 == (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM) ) {
				pos.colModule = MODULES_IN_MARGIN;
				pos.rowModule++;
			}
		}
		//end of column in rows without position detector	
		else if(pos.colModule == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN) ) {
			//next row is with position detector
			if(pos.rowModule + 1 >= (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM)) 
				pos.colModule = MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM;
			//next row is without position detector
			else 
				pos.colModule = MODULES_IN_MARGIN;
			pos.rowModule++;
		}
	}
	
	
	public int getPixel(int rowPixel, int colPixel) {
		return this.pixelMatrix[rowPixel][colPixel];
	}
	
	public int getWidth() {
	    return MODULES_IN_ENCODED_IMAGE_DIM * moduleSize;
	}

	public int getHeight() {
		return MODULES_IN_ENCODED_IMAGE_DIM * moduleSize;
	}
	
}
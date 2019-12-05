package com.pc.encoderDecoder;

import java.awt.image.BufferedImage;

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

	public int getDataLength() {
		return dataLength;
	}

	public byte[] getDecodedData() {
		return decodedData;
	}


	BufferedImage proccesedImage;
	int[][] pixelMatrix;
	int moduleSize;
	int dataLength;	
	public byte[] decodedData;
	
	static void checkForColumnEnd(Position pos) {
		
		if(pos.colModule == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ) {
			//end of column in rows of position detector (except for the last top one)	
			if(pos.rowModule < (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM) ||
					pos.rowModule >= (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ) {	
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
			if(pos.rowModule + 1 == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM)) 
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
	    return MODULES_IN_ENCODED_IMAGE_DIM;
	}

	public int getHeight() {
		return MODULES_IN_ENCODED_IMAGE_DIM;
	}
	
}
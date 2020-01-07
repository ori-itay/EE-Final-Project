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

	public int getDataLength() {
		return dataLength;
	}

	public byte[] getDecodedData() {
		return decodedData;
	}
	
	public int getImageHeight() {
		return imageHeight;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	public byte[] getIV() {
		return IV;
	}

	BufferedImage proccesedImage;
	int[][] pixelMatrix;
	int moduleSize = 0;
	int imageHeight;
	int imageWidth;
	int dataLength;	
	byte[] decodedData;
	byte[] IV;
	
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
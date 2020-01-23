package com.pc.encoderDecoder;

import java.awt.image.BufferedImage;
import static com.pc.configuration.Parameters.*;

public class StdImageSampler implements ImageSamplerInf{
	
	public BufferedImage getProccesedImage() {
		return proccesedImage;
	}

	public int[][] getPixelMatrix() {
		return pixelMatrix;
	}
	
	void setPixelMatrix(int[][] pixelMatrix) {
		this.pixelMatrix = pixelMatrix;
	}

	public int getModuleSize() {
		return moduleSize;
	}
	
	void setModuleSize(int moduleSize) {
		this.moduleSize =  moduleSize;
	}

	public byte[] getDecodedData() {
		return decodedData;
	}
	
	public byte[] getIV1() {
		return IV1;
	}

	void setIV1(byte[] iV1) {
		IV1 = iV1;
	}

	public byte[] getIV1Checksum() {
		return IV1Checksum;
	}

	void setIV1Checksum(byte[] iV1Checksum) {
		IV1Checksum = iV1Checksum;
	}

	public byte[] getIV2() {
		return IV2;
	}

	void setIV2(byte[] iV2) {
		IV2 = iV2;
	}

	public byte[] getIV2Checksum() {
		return IV2Checksum;
	}

	void setIV2Checksum(byte[] iV2Checksum) {
		IV2Checksum = iV2Checksum;
	}
	
	public int getPixel(int rowPixel, int colPixel) {
		return this.getPixelMatrix()[rowPixel][colPixel];
	}
	
	public int getWidth() {
	    return receivedImageDim;
	}

	public int getHeight() {
		return receivedImageDim;
	}

	public int getReceivedImageDim() {
		return receivedImageDim;
	}

	void setReceivedImageDim(int receivedImageDim) {
		this.receivedImageDim = receivedImageDim;
	}
	

	public int getModulesInDim() {
		return modulesInDim;
	}
	
	void setModulesInDim(int modulesInDim) {
		this.modulesInDim = modulesInDim;
	}

	BufferedImage proccesedImage;
	private int[][] pixelMatrix;
	private int receivedImageDim;
	private int moduleSize = 0;
	private byte[] decodedData;
	private byte[] IV1;
	private byte[] IV1Checksum;
	private byte[] IV2;
	private byte[] IV2Checksum;
	private int modulesInDim;
	
	

	public void checkForColumnEnd(Position pos) {
		imageCheckForColumnEnd(pos, modulesInDim);
	}
	
	static void imageCheckForColumnEnd(Position pos, int imageDim) {
		
		if(pos.colModule == (imageDim - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ) {
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
		else if(pos.colModule == (imageDim - MODULES_IN_MARGIN) ) {
			//next row is with position detector
			if(pos.rowModule + 1 >= (imageDim - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM)) 
				pos.colModule = MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM;
			//next row is without position detector
			else 
				pos.colModule = MODULES_IN_MARGIN;
			pos.rowModule++;
		}

	}

	public void setDecodedData(byte[] decodedData) {
		this.decodedData = decodedData;
	}

}

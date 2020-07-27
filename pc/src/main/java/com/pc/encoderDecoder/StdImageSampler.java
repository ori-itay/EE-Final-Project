package com.pc.encoderDecoder;

import java.awt.image.BufferedImage;

import com.pc.configuration.Parameters;

import static com.pc.configuration.Constants.*;

public class StdImageSampler implements ImageSamplerInf{

	public StdImageSampler() {
	}

	public static void imageCheckForColumnEnd(Position pos, int modulesInEncodedImageDim) {
	}

	public int[][] getPixelMatrix() {
		return pixelMatrix;
	}
	
	public void setPixelMatrix(int[][] pixelMatrix) {
		this.pixelMatrix = pixelMatrix;
	}

//	public int processedImage() {
//		return moduleSize;
//	}
//
	protected void setModuleSize(double moduleSize) {
		this.moduleSize =  moduleSize;
	}

	public double getModuleSize() { return moduleSize;	}

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

	void setDimsAndChecksum1(byte[] dims) {
		dims1 = dims;
	}

	void setDimsAndChecksum2(byte[] dims) {
		dims2 = dims;
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

	public byte[] getDimsAndChecksum1() {
		return dims1;
	}

	public byte[] getDimsAndChecksum2() {
		return dims2;
	}
	
	public int getPixel(int rowPixel, int colPixel) {
		//return this.getPixelMatrix()[rowPixel][colPixel];
		return tempOrigPixelMatrix[rowPixel][colPixel]; // TODO revert to original case in comment
	}
	public int getPixel(double rowPixel, double colPixel, boolean duplicateChannels, boolean radiusSample) {
		return 0;
	}
	
	public int getWidth() {
	    return this.width;
	}

	public int getHeight() {
		return this.height;
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
	
	protected void setModulesInDim(int modulesInDim) {
		this.modulesInDim = modulesInDim;
	}

	public int getModulesInMargin() { return modulesInMargin; }

	public void setModulesInMargin(int modulesInMargin) { this.modulesInMargin = modulesInMargin; }

	private int[][] pixelMatrix;
	private int receivedImageDim;

	private double moduleSize = 0.0;
	private byte[] decodedData;
	private byte[] IV1;
	private byte[] IV1Checksum;
	private byte[] IV2;

	public byte[] getIV() {
		return IV;
	}

	public void setIV(byte[] IV) {
		this.IV = IV;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	private byte[] IV;
	private int width, height;
	private byte[] IV2Checksum;
	private byte[] dims1;
	private byte[] dims2;
	private int modulesInDim;
	private int modulesInMargin;
	public int[][] tempOrigPixelMatrix;
	
	

	public void checkForColumnEnd(Position pos) {
		imageCheckForColumnEnd(pos, modulesInDim, this.modulesInMargin);
	}

	static void imageCheckForColumnEnd(Position pos, int imageDim, int effectiveModulesInMargin) {
		
		if(pos.colModule == (imageDim - effectiveModulesInMargin - MODULES_IN_POS_DET_DIM) ) {
			//end of row (last column) in rows of top position detectors (except for the last top one)
			if(pos.rowModule + 1 < effectiveModulesInMargin + MODULES_IN_POS_DET_DIM) {
				//next row is in row of position detector
				pos.colModule = (effectiveModulesInMargin + MODULES_IN_POS_DET_DIM);
				pos.rowModule++;
			}
			else if (pos.rowModule + 1 == (effectiveModulesInMargin + MODULES_IN_POS_DET_DIM) ) {
				//end of column when in last row of top position detector
				pos.colModule = effectiveModulesInMargin;
				pos.rowModule++;
			}
		}
		//end of row (last column) in rows without position detector on right
		else if(pos.colModule == (imageDim - effectiveModulesInMargin) ) {
			//next row is with position detector
			if(pos.rowModule + 1 >= (imageDim - effectiveModulesInMargin - MODULES_IN_POS_DET_DIM))
				pos.colModule = effectiveModulesInMargin + MODULES_IN_POS_DET_DIM;
			//next row is without position detector
			else 
				pos.colModule = effectiveModulesInMargin;
			pos.rowModule++;
		}

	}

	public void setDecodedData(byte[] decodedData) {
		this.decodedData = decodedData;
	}

}

package com.pc.encoderDecoder;

public class RotatedImageSampler extends StdImageSampler {
	
	int rotationCounterClockwise = 0;//by degrees
	
	public int getPixel(int rowPixel, int colPixel) {
		if (rotationCounterClockwise == 0)
			return pixelMatrix[rowPixel][colPixel];
		if (rotationCounterClockwise == 90)
			return pixelMatrix[colPixel][pixelMatrix.length-1-rowPixel];
		if (rotationCounterClockwise == 180)
			return pixelMatrix[pixelMatrix.length-1-rowPixel][pixelMatrix.length-1-colPixel];
		return pixelMatrix[pixelMatrix.length-1-colPixel][rowPixel]; //270 rotation
		
	}
	
}
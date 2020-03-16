package com.pc.encoderDecoder;

public class RotatedImageSampler extends StdImageSampler {
	
	int rotationCounterClockwise = 0;//by degrees
	
	public int getPixel(double rowPixelD, double colPixelD) {
		int rowPixel = (int) Math.floor(rowPixelD);
		int colPixel = (int) Math.floor(colPixelD);

		if (rotationCounterClockwise == 0)
			return getPixelMatrix()[rowPixel][colPixel];
		if (rotationCounterClockwise == 90)
			return getPixelMatrix()[colPixel][getPixelMatrix().length-1-rowPixel];
		if (rotationCounterClockwise == 180)
			return getPixelMatrix()[getReceivedImageDim()-1-rowPixel][getReceivedImageDim()-1-colPixel];
		return getPixelMatrix()[getReceivedImageDim()-1-colPixel][rowPixel]; //270 rotation
		
	}
	
}
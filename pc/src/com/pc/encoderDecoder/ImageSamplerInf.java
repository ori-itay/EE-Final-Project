package com.pc.encoderDecoder;

interface ImageSamplerInf{
	int getPixel(int rowPixel, int colPixel);
	int getWidth();
	int getHeight();
	
	public final static int MODULES_IN_ENCODED_IMAGE_DIM = 500;
	public final static int MODULES_IN_POS_DET_DIM = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	public final static int MODULES_IN_MARGIN = 2;
	public final static int NUM_OF_POSITION_DETECTORS = 3;
	public final static int DATA_LEN_ENCODING_LENGTH = 20;
	public static final int BITS_IN_BYTE = 8;
	public static final int MAX_ENCODED_LENGTH = 
			RotatedImageSampler.MODULES_IN_ENCODED_IMAGE_DIM*RotatedImageSampler.MODULES_IN_ENCODED_IMAGE_DIM 
			-4*RotatedImageSampler.MODULES_IN_MARGIN*RotatedImageSampler.MODULES_IN_ENCODED_IMAGE_DIM
			+4*RotatedImageSampler.MODULES_IN_MARGIN*RotatedImageSampler.MODULES_IN_MARGIN
			-RotatedImageSampler.MODULES_IN_POS_DET_DIM*RotatedImageSampler.MODULES_IN_POS_DET_DIM*
			RotatedImageSampler.NUM_OF_POSITION_DETECTORS-RotatedImageSampler.DATA_LEN_ENCODING_LENGTH;
}
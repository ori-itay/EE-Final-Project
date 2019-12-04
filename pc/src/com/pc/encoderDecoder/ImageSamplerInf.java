package com.pc.encoderDecoder;

interface ImageSamplerInf{
	int getPixel(int rowPixel, int colPixel);
	int getWidth();
	int getHeight();
	
	final static int MODULES_IN_ENCODED_IMAGE_DIM = 500;
	final static int MODULES_IN_POS_DET_DIM = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	final static int MODULES_IN_MARGIN = 2;
	final static int NUM_OF_POSITION_DETECTORS = 3;
	final static int DATA_LEN_ENCODING_LENGTH = 20;
	static final int BITS_IN_BYTE = 8;
}


class Position {
	int rowModule;
	int colModule;
}
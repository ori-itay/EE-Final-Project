package com.pc.configuration;

public class Constants {
	public static final int ivLength = 12;
	public static final int maxImageSizeBytes = 100*100*64*8; //width * height * levels * bytes
	public static final int ENCODING_COLOR_LEVELS = 32;
	public static final int GREY_SCALE_DELTA = (int) Math.ceil(256 / ENCODING_COLOR_LEVELS);
	public static final int PIXELS_IN_MODULE = 3;
	
	
	public final static int DATA_LEN_ENCODING_LENGTH_BITS = 20;
	public final static int DATA_LEN_ENCODING_LENGTH_MODULES = (int) Math.ceil(Math.log(DATA_LEN_ENCODING_LENGTH_BITS)/ Math.log(2));
	public final static int MODULES_IN_ENCODED_IMAGE_DIM = 500;
	public final static int MODULES_IN_POS_DET_DIM = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	public final static int MODULES_IN_MARGIN = 2;
	public final static int NUM_OF_POSITION_DETECTORS = 3;
	public static final int BITS_IN_BYTE = 8;
	public static final int MAX_ENCODED_LENGTH = MODULES_IN_ENCODED_IMAGE_DIM*MODULES_IN_ENCODED_IMAGE_DIM 
			- 4*MODULES_IN_MARGIN*MODULES_IN_ENCODED_IMAGE_DIM + 4*MODULES_IN_MARGIN*MODULES_IN_MARGIN
			- MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS - DATA_LEN_ENCODING_LENGTH_MODULES;
	
	public static final int ENCODING_BIT_GROUP_SIZE = (int) (Math.log(ENCODING_COLOR_LEVELS)/ Math.log(2) );
	public static final int BIT_GROUP_MASK_OF_ONES = (1 << ENCODING_BIT_GROUP_SIZE) -1;	
	
}

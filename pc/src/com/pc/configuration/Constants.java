package com.pc.configuration;

public class Constants {
	public static final int ivLength = 12;
	public static final int CHECKSUM_LENGTH = 1;
	public static final int IMAGE_DIMS_ENCODING_LENGTH = 4;
	public static final String ENCRYPTION_ALGORITHM = "AES";
	public static final int ENCODING_COLOR_LEVELS = 64;
	public static final int GREY_SCALE_DELTA = Math.floorDiv(255 , (ENCODING_COLOR_LEVELS-1));
	public static final int PIXELS_IN_MODULE = 3;
	
	public static final int BITS_IN_BYTE = 8;
	public static final int RGB_PIXEL_DATA_SIZE = 32;
	
	
	public static final int ENCODING_BIT_GROUP_SIZE = (int) (Math.log(ENCODING_COLOR_LEVELS)/ Math.log(2) );
	public static final int BIT_GROUP_MASK_OF_ONES = (1 << ENCODING_BIT_GROUP_SIZE) -1;	
	
	public final static int DATA_LEN_ENCODING_LENGTH_BYTES = 4;
	public final static int DATA_LEN_ENCODING_LENGTH_MODULES = (int) Math.ceil((float)(DATA_LEN_ENCODING_LENGTH_BYTES*BITS_IN_BYTE) / ENCODING_BIT_GROUP_SIZE);
	public final static int MODULES_IN_ENCODED_IMAGE_DIM = 500;
	public final static int MODULES_IN_POS_DET_DIM = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	public final static int MODULES_IN_MARGIN = 2;
	public final static int NUM_OF_POSITION_DETECTORS = 3;

	public static final int MAX_ENCODED_LENGTH = ENCODING_BIT_GROUP_SIZE*(MODULES_IN_ENCODED_IMAGE_DIM*MODULES_IN_ENCODED_IMAGE_DIM 
			- 4*MODULES_IN_MARGIN*(MODULES_IN_ENCODED_IMAGE_DIM -MODULES_IN_MARGIN)
			- MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS)
			- 2*BITS_IN_BYTE*(ivLength+CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH+CHECKSUM_LENGTH);
	
	public static final int MAX_ENCODED_LENGTH_BYTES = MAX_ENCODED_LENGTH / BITS_IN_BYTE;
	
	public static final int MAX_IMAGE_DIMENSION_SIZE = 1000;
}

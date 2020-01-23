package com.pc.configuration;

public class Constants {
	
	/* Checksum */
	public static final int CHECKSUM_LENGTH = 1;
	
	/* Dimensions */
	public static final int IMAGE_DIMENSION_ENCODING_LENGTH = 2;
	public static final int IMAGE_DIMS_ENCODING_LENGTH = 2 * IMAGE_DIMENSION_ENCODING_LENGTH;
	
	/* Encoding */
	public static int MODULES_IN_ENCODED_IMAGE_DIM;
	public static int GREY_SCALE_DELTA = Math.floorDiv(255 , (Parameters.encodingColorLevels-1));
	public static int ENCODING_BIT_GROUP_SIZE = (int) (Math.log(Parameters.encodingColorLevels)/ Math.log(2) );
	public static int BIT_GROUP_MASK_OF_ONES = (1 << ENCODING_BIT_GROUP_SIZE) -1;	
		
	public final static int MODULES_IN_POS_DET_DIM = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	public final static int NUM_OF_POSITION_DETECTORS = 3;

	public static final int BITS_IN_BYTE = 8;
	public static final int RGB_PIXEL_DATA_SIZE = 32;
	
	public static final int MAX_IMAGE_DIMENSION_SIZE = 1000;
	
	public static final int MAX_ENCODED_LENGTH_BYTES = MAX_IMAGE_DIMENSION_SIZE*MAX_IMAGE_DIMENSION_SIZE*4;
	
}

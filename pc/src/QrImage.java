import java.awt.image.BufferedImage;


interface ImageSampler{
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

class StdImageSampler implements ImageSampler{
	
	BufferedImage proccesedImage;
	int[][] pixelMatrix;
	int moduleSize;
	int dataLength;	
	byte[] decodedData;
	
	static void checkForColumnEnd(Position pos) {
		
		if(pos.colModule == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ) {
			//end of column in rows of position detector (except for the last top one)	
			if(pos.rowModule < (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM) ||
					pos.rowModule >= (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ) {	
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
			if(pos.rowModule + 1 == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM)) 
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
	    return MODULES_IN_ENCODED_IMAGE_DIM;
	}

	public int getHeight() {
		return MODULES_IN_ENCODED_IMAGE_DIM;
	}
	
}

class RotatedImageSampler extends StdImageSampler {
	
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

class Position {
	int rowModule;
	int colModule;
}
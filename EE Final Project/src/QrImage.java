import java.awt.image.BufferedImage;

public class QrImage {
	
	final static int MODULES_IN_ENCODED_IMAGE_DIM = 500;
	final static int MODULES_IN_POS_DET_DIM = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	final static int MODULES_IN_MARGIN = 2;
	final static int NUM_OF_POSITION_DETECTORS = 3;
	final static int DATA_LEN_ENCODING_LENGTH = 20;
	static final int BITS_IN_BYTE = 8;
	
	BufferedImage prossecedImage;
	int[][] pixelMatrix;
	int moduleSize;
	int dataLength;	
	
	static void checkForColumnEnd(Position pos) {
		
		if(pos.colModule == (QrImage.MODULES_IN_ENCODED_IMAGE_DIM - QrImage.MODULES_IN_MARGIN) ) {
			//end of column in rows of position detector (except for the last top one)	
			if(pos.rowModule < (QrImage.MODULES_IN_MARGIN + QrImage.MODULES_IN_POS_DET_DIM) ||
					pos.rowModule > (QrImage.MODULES_IN_ENCODED_IMAGE_DIM - QrImage.MODULES_IN_MARGIN - QrImage.MODULES_IN_POS_DET_DIM) ) {	
				pos.colModule = (QrImage.MODULES_IN_MARGIN + QrImage.MODULES_IN_POS_DET_DIM);
				pos.rowModule++;
			}
			//end of column when in last row of position detector	
			else if (pos.rowModule == (QrImage.MODULES_IN_MARGIN + QrImage.MODULES_IN_POS_DET_DIM) ) {
				pos.colModule = QrImage.MODULES_IN_MARGIN;
				pos.rowModule++;
			}
		}
		//end of column in rows without position detector	
		else if(pos.colModule == (QrImage.MODULES_IN_ENCODED_IMAGE_DIM - QrImage.MODULES_IN_MARGIN) ) {
			//next row is with position detector
			if(pos.rowModule == (QrImage.MODULES_IN_ENCODED_IMAGE_DIM - QrImage.MODULES_IN_MARGIN - QrImage.MODULES_IN_POS_DET_DIM)) 
				pos.colModule = QrImage.MODULES_IN_MARGIN + QrImage.MODULES_IN_POS_DET_DIM;
			//next row is without position detector
			else 
				pos.colModule = QrImage.MODULES_IN_MARGIN;
			pos.rowModule++;
		}
	}
}

class Position {
	int rowModule;
	int colModule;
}
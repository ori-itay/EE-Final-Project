import java.awt.image.BufferedImage;


public class DisplayEncoder {
	
	public final static int MODULES_IN_ENCODED_IMAGE_DIM = 250;
	public final static int MODULES_IN_MARGIN = 2;
	public final static int PIXELS_IN_MODULE = 3;
	public final static int BLACK = 0xFF000000;
	public final static int WHITE = 0x0000000;
	
	public static BufferedImage encodeBytes(byte[] binaryData) {
		
		//allocate space including white margins
		BufferedImage image = new BufferedImage(MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE,
				MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE, BufferedImage.TYPE_INT_ARGB);
		//create margins
		//create position detector
		createPositionDetector(image);
		
		//encode data length - 20 bits - masked
		//encode data - masked
		
		return image;
	}

	private static void createPositionDetector(BufferedImage image) {
		
		final int MODULES_IN_POS_DET = 7;
		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;
		//final int POS_DET_CENTER_OFFSET_1 = 2; final int POS_DET_CENTER_OFFSET_2 = 3;
		
		int row = MODULES_IN_MARGIN * PIXELS_IN_MODULE;	int col = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowModuleOffset = 0, colModuleOffset = 0;
		
		for(;rowModuleOffset<  MODULES_IN_POS_DET; rowModuleOffset++) {
			for(;colModuleOffset<  MODULES_IN_POS_DET; colModuleOffset++) {
				
				if( (rowModuleOffset == MID_LAYER_OFFSET_1 || rowModuleOffset == MID_LAYER_OFFSET_2) &&
						(colModuleOffset == MID_LAYER_OFFSET_1 || colModuleOffset == MID_LAYER_OFFSET_2)) {
					colorModule(image, WHITE, row + rowModuleOffset * PIXELS_IN_MODULE,
							col + colModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE);
				}
				else {
					colorModule(image, BLACK, row + rowModuleOffset * PIXELS_IN_MODULE,
							col + colModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE);
				}
//				// position detector margins
//				if(rowModuleOffset == 0 || colModuleOffset ==0 || 
//						rowModuleOffset == MODULES_IN_POS_DET - 1 || colModuleOffset == MODULES_IN_POS_DET - 1) {
//					
//					colorModule(image, BLACK, row + rowModuleOffset * PIXELS_IN_MODULE,
//							col + colModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE);
//				}
//				// position detector center
//				else if( (rowModuleOffset == POS_DET_CENTER_OFFSET_1 || rowModuleOffset == POS_DET_CENTER_OFFSET_2) &&
//						(colModuleOffset == POS_DET_CENTER_OFFSET_1 || colModuleOffset == POS_DET_CENTER_OFFSET_2)) {
//					colorModule(image, BLACK, row + rowModuleOffset * PIXELS_IN_MODULE,
//							col + colModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE);
//				}
//				// position detector middle layer
//				else {
//					colorModule(image, WHITE, row + rowModuleOffset * PIXELS_IN_MODULE,
//							col + colModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE);
//				}
			}
		}		
	}

	private static void colorModule(BufferedImage image, int color, int topRowInd, int leftColInd, int pixelsInModule) {

		for(int rowOffset = 0; rowOffset < pixelsInModule; rowOffset++) {
			for(int colOffset = 0; colOffset < pixelsInModule; colOffset++) {
				image.setRGB(leftColInd + colOffset, topRowInd + rowOffset, color);
			}
		}
	}
	
}
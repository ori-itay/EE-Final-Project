import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.IOException;


enum ROW {
	  WITH_POS_DET,
	  WITHOUT_POS_DET,
	  SAME_ROW
	}

public class DisplayEncoder {
	
	final static int MODULES_IN_ENCODED_IMAGE_DIM = 700;
	final static int MODULES_IN_MARGIN = 2;
	final static int MODULES_IN_POS_DET = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	final static int PIXELS_IN_MODULE = 3;
	final static int BLACK = 0xFF000000;
	final static int WHITE = 0x0000000;
	final static int DATA_LEN_ENCODING_LENGTH = 20;
	
	public static void main(String... args) throws Exception {
		String testData = "BLABLAa";
		encodeBytes(testData);
	}
	
	public static BufferedImage encodeBytes(String binaryData) throws Exception {
		
		//allocate space including white margins
		BufferedImage image = new BufferedImage(MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE,
				MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE, BufferedImage.TYPE_INT_ARGB);		 
		// Clear the background with white
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE, MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE);
		//create position detector
		createPositionDetectors(image);
		//encode data length - 20 bits - masked
		encodeDataLen(image, binaryData.length());
		//encode data - masked
		
		//print image for testing
		File newPathQr = new File("C:\\Users\\user\\Downloads\\new qrcode.png");
		ImageIO.write(image, "png", newPathQr);

		return image;
	}


	private static void encodeDataLen(BufferedImage image, int length) throws Exception {
		//LSB is encoded as the first (leftmost) bit
		int i;
		int row = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int col = (MODULES_IN_MARGIN + MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
		final int MASK = 1;	
		
		if(length>0xFFFFF)
			throw new Exception("Data is too large to be encoded!");
		
		for(i = 0; i < DATA_LEN_ENCODING_LENGTH; i++) {
			if((MASK&length) == 1)
				colorModule(image, BLACK, row, col);
			col+= PIXELS_IN_MODULE;
			//maybe remove the following because this will probably wont be the situation
			switch(checkForColumnEnd(row, col)){
			case WITH_POS_DET:
				col = (MODULES_IN_MARGIN + MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
				row+= PIXELS_IN_MODULE;
				break;
			case WITHOUT_POS_DET:
				col = 0;
				row+= PIXELS_IN_MODULE;	
				break;
			default:
				break;				
			}
			length = length>>1;
		}
		
	}

	private static ROW checkForColumnEnd(int row, int col) {
		if(col == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET) * PIXELS_IN_MODULE &&
				(row < (MODULES_IN_MARGIN + MODULES_IN_POS_DET) * PIXELS_IN_MODULE || 
						row > (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET) * PIXELS_IN_MODULE) ) {
			return ROW.WITH_POS_DET;
		}
		else if(col == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN) * PIXELS_IN_MODULE) {
			return ROW.WITHOUT_POS_DET;
		}
		return ROW.SAME_ROW;
	}

	private static void createPositionDetectors(BufferedImage image) {
		
		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;
		
		int rowTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowTopRight = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopRight = (MODULES_IN_ENCODED_IMAGE_DIM-MODULES_IN_MARGIN-MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
		int rowBottomLeft = (MODULES_IN_ENCODED_IMAGE_DIM-MODULES_IN_MARGIN-MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
		int colBottomLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowModuleOffset, colModuleOffset;
		
		for(rowModuleOffset = 0; rowModuleOffset< MODULES_IN_POS_DET; rowModuleOffset++) {
			for(colModuleOffset = 0; colModuleOffset< MODULES_IN_POS_DET; colModuleOffset++) {
				
				if( !((rowModuleOffset == MID_LAYER_OFFSET_1 || rowModuleOffset == MID_LAYER_OFFSET_2) &&
						(colModuleOffset >= MID_LAYER_OFFSET_1 && colModuleOffset <= MID_LAYER_OFFSET_2)) &&
						!((rowModuleOffset > MID_LAYER_OFFSET_1 && rowModuleOffset < MID_LAYER_OFFSET_2) &&
								(colModuleOffset == MID_LAYER_OFFSET_1 || colModuleOffset == MID_LAYER_OFFSET_2))) {
					colorModule(image, BLACK, rowTopLeft + rowModuleOffset * PIXELS_IN_MODULE,
							colTopLeft + colModuleOffset * PIXELS_IN_MODULE);
					colorModule(image, BLACK, rowTopRight + rowModuleOffset * PIXELS_IN_MODULE,
							colTopRight + colModuleOffset * PIXELS_IN_MODULE);
					colorModule(image, BLACK, rowBottomLeft + rowModuleOffset * PIXELS_IN_MODULE,
							colBottomLeft + colModuleOffset * PIXELS_IN_MODULE);
				}
			}
		}		
	}

	private static void colorModule(BufferedImage image, int color, int topRowInd, int leftColInd) {

		int rowOffset, colOffset;
		
		for(rowOffset = 0; rowOffset < PIXELS_IN_MODULE; rowOffset++) {
			for(colOffset = 0; colOffset < PIXELS_IN_MODULE; colOffset++) {
				image.setRGB(leftColInd + colOffset, topRowInd + rowOffset, color);
			}
		}
	}
	
}
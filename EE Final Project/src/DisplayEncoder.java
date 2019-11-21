import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;


enum ROW {
	  WITH_POS_DET,
	  WITHOUT_POS_DET,
	  SAME_ROW
	}

public class DisplayEncoder {
	
	final static int MODULES_IN_ENCODED_IMAGE_DIM = 500;
	final static int MODULES_IN_MARGIN = 2;
	final static int MODULES_IN_POS_DET_DIM = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	final static int PIXELS_IN_MODULE = 3;
	final static int NUM_OF_POSITION_DETECTORS = 3;
	//final static int BLACK = 0xFF000000;
	//final static int WHITE = 0x0000000;
	final static int DATA_LEN_ENCODING_LENGTH = 20;
	
	public static void main(String... args) throws Exception {
		String testData = "BLABLAabbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbccccccccccccccdddd";
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
		g.setColor(Color.BLACK);
		//create position detector
		createPositionDetectors(image, g);
		//encode data length - 20 bits - masked
		Position pos = new Position();
		encodeDataLen(image, g, binaryData.length(), pos);
		//encode data - masked
		encodeData(image, g, binaryData, pos);
		
		//print image for testing
		File newPathQr = new File("C:\\Users\\user\\Downloads\\new qrcode.png");
		ImageIO.write(image, "png", newPathQr);

		return image;
	}


	private static void encodeData(BufferedImage image, Graphics2D g, String binaryData, Position pos) {
		
		final int BITS_IN_BYTE = 8;
		int mask, bitInd;
		boolean currBitIs1;
		
		byte[] stringAsBytes = binaryData.getBytes();
		for (byte currByte:stringAsBytes){
			mask = 1;
			for(bitInd = 0; bitInd < BITS_IN_BYTE; bitInd++) {
				currBitIs1 = (currByte&mask) == 1;
			    encodeBit(image, g, pos, currBitIs1);
			    checkForColumnEnd(pos);
			    mask = mask<<1;
			}
		}	
	}

	private static void encodeDataLen(BufferedImage image, Graphics2D g, int length, Position pos) throws Exception {
		//LSB is encoded as the first (leftmost) bit
		int i;
		boolean currBitIs1;
		
		pos.row = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		pos.col = (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM) * PIXELS_IN_MODULE;
		pos.colModul = MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM;
		final int LSB_MASK = 1;	
		
		if(length>MODULES_IN_ENCODED_IMAGE_DIM*MODULES_IN_ENCODED_IMAGE_DIM - 4*MODULES_IN_MARGIN*MODULES_IN_ENCODED_IMAGE_DIM
				-MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS)
			throw new Exception("Data is too large to be encoded!");
		
		for(i = 0; i < DATA_LEN_ENCODING_LENGTH; i++) {
			currBitIs1 = (LSB_MASK&length) == 1;
			encodeBit(image, g, pos, currBitIs1);			
			//maybe remove the following because this will probably wont be the situation
			checkForColumnEnd(pos);
			length = length>>1;
		}
	}


	private static void encodeBit(BufferedImage image, Graphics2D g, Position pos, boolean currBitIs1) {

		boolean toggleBit;
		
		toggleBit = pos.colModul%3 == 0;	
		if(currBitIs1 ^ toggleBit)
			g.fillRect(pos.col, pos.row, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
		pos.colModul++;
		pos.col+= PIXELS_IN_MODULE;
	}

	private static void checkForColumnEnd(Position pos) {
		
		if(pos.colModul == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN) ) {
			//end of column in rows of position detector (except for the last top one)	
			if(pos.rowModul < (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM) ||
					pos.row > (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ) {	
				pos.colModul = (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM);
				pos.row+= PIXELS_IN_MODULE;
				pos.rowModul++;
			}
			//end of column when in last row of position detector	
			else if (pos.rowModul == (MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM) ) {
				pos.colModul = 0;
				pos.row+= PIXELS_IN_MODULE;
				pos.rowModul++;
			}
			pos.col = pos.colModul * PIXELS_IN_MODULE;
		}
		//end of column in rows without position detector	
		else if(pos.colModul == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN) ) {
			//next row is with position detector
			if(pos.rowModul == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM)) 
				pos.colModul = MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM;
			//next row is without position detector
			else 
				pos.colModul = MODULES_IN_MARGIN;
			pos.row+= PIXELS_IN_MODULE;
			pos.rowModul++;
			pos.col = pos.colModul * PIXELS_IN_MODULE;
		}
	}

	private static void createPositionDetectors(BufferedImage image, Graphics2D g) {
		
		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;
		
		int rowTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowTopRight = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopRight = (MODULES_IN_ENCODED_IMAGE_DIM-MODULES_IN_MARGIN-MODULES_IN_POS_DET_DIM) * PIXELS_IN_MODULE;
		int rowBottomLeft = (MODULES_IN_ENCODED_IMAGE_DIM-MODULES_IN_MARGIN-MODULES_IN_POS_DET_DIM) * PIXELS_IN_MODULE;
		int colBottomLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowModuleOffset, colModuleOffset;
		
		for(rowModuleOffset = 0; rowModuleOffset< MODULES_IN_POS_DET_DIM; rowModuleOffset++) {
			for(colModuleOffset = 0; colModuleOffset< MODULES_IN_POS_DET_DIM; colModuleOffset++) {
				
				if( !((rowModuleOffset == MID_LAYER_OFFSET_1 || rowModuleOffset == MID_LAYER_OFFSET_2) &&
						(colModuleOffset >= MID_LAYER_OFFSET_1 && colModuleOffset <= MID_LAYER_OFFSET_2)) &&
						!((rowModuleOffset > MID_LAYER_OFFSET_1 && rowModuleOffset < MID_LAYER_OFFSET_2) &&
								(colModuleOffset == MID_LAYER_OFFSET_1 || colModuleOffset == MID_LAYER_OFFSET_2))) {
					g.fillRect(colTopLeft + colModuleOffset * PIXELS_IN_MODULE,
							rowTopLeft + rowModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
					g.fillRect(colTopRight + colModuleOffset * PIXELS_IN_MODULE,
							 rowTopRight + rowModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
					g.fillRect(colBottomLeft + colModuleOffset * PIXELS_IN_MODULE,
							rowBottomLeft + rowModuleOffset * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
				}
			}
		}		
	}		
}

class Position{
	int row;
	int col;
	int rowModul;
	int colModul;
}
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
	
	final static int MODULES_IN_ENCODED_IMAGE_DIM = 700;
	final static int MODULES_IN_MARGIN = 2;
	final static int MODULES_IN_POS_DET = 7; //maybe change to 8 due to white margins from inside of position detector and draw them in position detector creation
	final static int PIXELS_IN_MODULE = 3;
	//final static int BLACK = 0xFF000000;
	//final static int WHITE = 0x0000000;
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
		g.setColor(Color.BLACK);
		//create position detector
		createPositionDetectors(image, g);
		//encode data length - 20 bits - masked
		encodeDataLen(image, g, binaryData.length());
		//encode data - masked
		
		//print image for testing
		File newPathQr = new File("C:\\Users\\user\\Downloads\\new qrcode.png");
		ImageIO.write(image, "png", newPathQr);

		return image;
	}


	private static void encodeDataLen(BufferedImage image, Graphics2D g, int length) throws Exception {
		//LSB is encoded as the first (leftmost) bit
		int i;
		boolean currBitIs1;
		Position p = new Position();
		p.row = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		p.col = (MODULES_IN_MARGIN + MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
		p.colModul = MODULES_IN_MARGIN + MODULES_IN_POS_DET;
		final int LSB_MASK = 1;	
		
		if(length>0xFFFFF)
			throw new Exception("Data is too large to be encoded!");
		
		for(i = 0; i < DATA_LEN_ENCODING_LENGTH; i++) {
			currBitIs1 = (LSB_MASK&length) == 1;
			encodeBit(image, g, p, currBitIs1);			
			//maybe remove the following because this will probably wont be the situation
			checkForColumnEnd(p);
			length = length>>1;
		}
		
	}


	private static void encodeBit(BufferedImage image, Graphics2D g, Position p, boolean currBitIs1) {

		boolean toggleBit;
		
		toggleBit = p.colModul%3 == 0;
		p.colModul++;
		if(currBitIs1 ^ toggleBit)
			g.fillRect(p.col, p.row, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
		p.col+= PIXELS_IN_MODULE;
	}

	private static void checkForColumnEnd(Position p) {
		if(p.col == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET) * PIXELS_IN_MODULE &&
				(p.row < (MODULES_IN_MARGIN + MODULES_IN_POS_DET) * PIXELS_IN_MODULE || 
						p.row > (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN - MODULES_IN_POS_DET) * PIXELS_IN_MODULE) ) {
			p.col = (MODULES_IN_MARGIN + MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
			p.row+= PIXELS_IN_MODULE;
		}
		else if(p.col == (MODULES_IN_ENCODED_IMAGE_DIM - MODULES_IN_MARGIN) * PIXELS_IN_MODULE) {
			p.col = 0;
			p.row+= PIXELS_IN_MODULE;
		}
	}

	private static void createPositionDetectors(BufferedImage image, Graphics2D g) {
		
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
	//int rowModul;
	int colModul;
}
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
	
	private final static int PIXELS_IN_MODULE = 3;
	
	public static void main(String... args) throws Exception {
		//String testData = "BLABLAabbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbccccccccccccccdddd";
		String testData = "BLABLA";
		BufferedImage encodedImage = encodeBytes(testData);
		
		//print image for testing
		File newPathQr = new File("C:\\Users\\user\\Downloads\\new qrcode.png");
		ImageIO.write(encodedImage, "png", newPathQr);
	}
	
	public static BufferedImage encodeBytes(String binaryData) throws Exception {
		
		//allocate space including white margins
		BufferedImage image = new BufferedImage(QrImage.MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE,
				QrImage.MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE, BufferedImage.TYPE_INT_ARGB);		 
		// Clear the background with white
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, QrImage.MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE, QrImage.MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE);
		g.setColor(Color.BLACK);
		//create position detector
		createPositionDetectors(image, g);
		//encode data length - 20 bits - masked
		Position pos = new Position();
		encodeDataLen(image, g, binaryData.length() * QrImage.BITS_IN_BYTE, pos);
		//encode data - masked
		encodeData(image, g, binaryData, pos);
		
		return image;
	}


	private static void encodeData(BufferedImage image, Graphics2D g, String binaryData, Position pos) {
		
		
		int mask, bitInd;
		boolean currBitIs1;
		
		byte[] stringAsBytes = binaryData.getBytes();
		for (int i = 0; i<stringAsBytes.length; i++){
			mask = 1;
			for(bitInd = 0; bitInd < QrImage.BITS_IN_BYTE; bitInd++) {
				currBitIs1 = (stringAsBytes[i]&mask) != 0;
			    encodeBit(image, g, pos, currBitIs1);
			    QrImage.checkForColumnEnd(pos);
			    mask = mask<<1;
			}
		}	
	}

	private static void encodeDataLen(BufferedImage image, Graphics2D g, int length, Position pos) throws Exception {
		//LSB is encoded as the first (leftmost) bit
		int i;
		boolean currBitIs1;
		
		pos.rowModule = QrImage.MODULES_IN_MARGIN;		
		pos.colModule = QrImage.MODULES_IN_MARGIN + QrImage.MODULES_IN_POS_DET_DIM;
		final int LSB_MASK = 1;	
		
		if(length>QrImage.MODULES_IN_ENCODED_IMAGE_DIM*QrImage.MODULES_IN_ENCODED_IMAGE_DIM - 
				4*(QrImage.MODULES_IN_MARGIN*QrImage.MODULES_IN_ENCODED_IMAGE_DIM + QrImage.MODULES_IN_MARGIN*QrImage.MODULES_IN_MARGIN)
				-QrImage.MODULES_IN_POS_DET_DIM*QrImage.MODULES_IN_POS_DET_DIM*QrImage.NUM_OF_POSITION_DETECTORS)
			throw new Exception("Data is too large to be encoded!");
		
		for(i = 0; i < QrImage.DATA_LEN_ENCODING_LENGTH; i++) {
			currBitIs1 = (LSB_MASK&length) == 1;
			encodeBit(image, g, pos, currBitIs1);			
			//maybe remove the following because this will probably wont be the situation
			QrImage.checkForColumnEnd(pos);
			length = length>>1;
		}
	}


	private static void encodeBit(BufferedImage image, Graphics2D g, Position pos, boolean currBitIs1) {

		boolean toggleBit;
		
		toggleBit = pos.colModule%3 == 0;	
		if(currBitIs1 ^ toggleBit)
			g.fillRect(pos.colModule * PIXELS_IN_MODULE, pos.rowModule * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
		pos.colModule++;
	}



	private static void createPositionDetectors(BufferedImage image, Graphics2D g) {
		
		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;
		
		int rowTopLeft = QrImage.MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopLeft = QrImage.MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowTopRight = QrImage.MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopRight = (QrImage.MODULES_IN_ENCODED_IMAGE_DIM-QrImage.MODULES_IN_MARGIN-QrImage.MODULES_IN_POS_DET_DIM) * PIXELS_IN_MODULE;
		int rowBottomLeft = (QrImage.MODULES_IN_ENCODED_IMAGE_DIM-QrImage.MODULES_IN_MARGIN-QrImage.MODULES_IN_POS_DET_DIM) * PIXELS_IN_MODULE;
		int colBottomLeft = QrImage.MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowModuleOffset, colModuleOffset;
		
		for(rowModuleOffset = 0; rowModuleOffset< QrImage.MODULES_IN_POS_DET_DIM; rowModuleOffset++) {
			for(colModuleOffset = 0; colModuleOffset< QrImage.MODULES_IN_POS_DET_DIM; colModuleOffset++) {
				
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
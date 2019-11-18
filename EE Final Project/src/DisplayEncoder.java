import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import java.io.IOException;


public class DisplayEncoder {
	
	public final static int MODULES_IN_ENCODED_IMAGE_DIM = 700;
	public final static int MODULES_IN_MARGIN = 2;
	public final static int PIXELS_IN_MODULE = 3;
	public final static int BLACK = 0xFF000000;
	public final static int WHITE = 0x0000000;
	
	public static void main(String... args) throws IOException {
		byte[] testData = new byte[10];
		encodeBytes(testData);
	}
	
	public static BufferedImage encodeBytes(byte[] binaryData) {
		
		//allocate space including white margins
		BufferedImage image = new BufferedImage(MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE,
				MODULES_IN_ENCODED_IMAGE_DIM*PIXELS_IN_MODULE, BufferedImage.TYPE_INT_ARGB);
		//create margins
		//create position detector
		createPositionDetectors(image);
		//encode data length - 20 bits - masked
		//encode data - masked
		
		File newPathQr = new File("C:\\Users\\user\\Downloads\\new qrcode.png");
		try {
			ImageIO.write(image, "png", newPathQr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return image;
	}

	private static void createPositionDetectors(BufferedImage image) {
		
		final int MODULES_IN_POS_DET = 7;
		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;
		
		int rowTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowTopRight = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopRight = (MODULES_IN_ENCODED_IMAGE_DIM-MODULES_IN_MARGIN-MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
		int rowBottomLeft = (MODULES_IN_ENCODED_IMAGE_DIM-MODULES_IN_MARGIN-MODULES_IN_POS_DET) * PIXELS_IN_MODULE;
		int colBottomLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowModuleOffset, colModuleOffset;
		
		for(rowModuleOffset = 0;rowModuleOffset<  MODULES_IN_POS_DET; rowModuleOffset++) {
			for(colModuleOffset = 0;colModuleOffset<  MODULES_IN_POS_DET; colModuleOffset++) {
				
				if( (rowModuleOffset == MID_LAYER_OFFSET_1 || rowModuleOffset == MID_LAYER_OFFSET_2) &&
						(colModuleOffset >= MID_LAYER_OFFSET_1 && colModuleOffset <= MID_LAYER_OFFSET_2)) {
					colorModule(image, WHITE, rowTopLeft + rowModuleOffset * PIXELS_IN_MODULE,
							colTopLeft + colModuleOffset * PIXELS_IN_MODULE);
					colorModule(image, WHITE, rowTopRight + rowModuleOffset * PIXELS_IN_MODULE,
							colTopRight + colModuleOffset * PIXELS_IN_MODULE);
					colorModule(image, WHITE, rowBottomLeft + rowModuleOffset * PIXELS_IN_MODULE,
							colBottomLeft + colModuleOffset * PIXELS_IN_MODULE);
				}
				else if( (rowModuleOffset > MID_LAYER_OFFSET_1 && rowModuleOffset < MID_LAYER_OFFSET_2) &&
						(colModuleOffset == MID_LAYER_OFFSET_1 || colModuleOffset == MID_LAYER_OFFSET_2)) {
					colorModule(image, WHITE, rowTopLeft + rowModuleOffset * PIXELS_IN_MODULE,
							colTopLeft + colModuleOffset * PIXELS_IN_MODULE);	
					colorModule(image, WHITE, rowTopRight + rowModuleOffset * PIXELS_IN_MODULE,
							colTopRight + colModuleOffset * PIXELS_IN_MODULE);
					colorModule(image, WHITE, rowBottomLeft + rowModuleOffset * PIXELS_IN_MODULE,
							colBottomLeft + colModuleOffset * PIXELS_IN_MODULE);
				}
				else {
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
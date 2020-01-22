package com.pc.encoderDecoder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import static com.pc.configuration.Parameters.*;


enum ROW {
	  WITH_POS_DET,
	  WITHOUT_POS_DET,
	  SAME_ROW
	}

public class DisplayEncoder {
	
	static int MODULES_IN_ENCODED_IMAGE_DIM;
	

	public static BufferedImage encodeBytes(byte[] binaryData, byte[] IV, byte[] ivchecksum) throws Exception {
		MODULES_IN_ENCODED_IMAGE_DIM = computeMinDimension(binaryData.length);
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
		Position pos = new Position(MODULES_IN_MARGIN, MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM);
		encodeData(image,g, IV, pos); //encode IV first time
		encodeData(image,g, ivchecksum, pos); //encode IV checksum first time
		encodeData(image, g, binaryData, pos); 	//encode actual picture data
		encodeData(image,g, IV, pos); //encode IV second time
		encodeData(image,g, ivchecksum, pos); //encode IV checksum second time
		
		return image;
	}
	
	private static int computeMinDimension(int dataLength) {
		int modulesForEncoding = (int) Math.ceil((float)(dataLength*BITS_IN_BYTE + 2*(ivLength + CHECKSUM_LENGTH)) /
				ENCODING_BIT_GROUP_SIZE);
		int dim = (int) Math.ceil(Math.sqrt(modulesForEncoding)) + 2*(MODULES_IN_POS_DET_DIM+MODULES_IN_MARGIN); // initial guess
		
		while(computeMaxEncodedLength(dim) < dataLength) 
			dim++;
		
		while(computeMaxEncodedLength(dim-1) >= dataLength) 
			dim--;
		
		
		return dim;
	}

	private static int computeMaxEncodedLength(int dim) {
		return (ENCODING_BIT_GROUP_SIZE*((dim*dim 
				- 4*MODULES_IN_MARGIN*(dim -MODULES_IN_MARGIN)
				- MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS)
				- 2*BITS_IN_BYTE*(ivLength+CHECKSUM_LENGTH+IMAGE_DIMS_ENCODING_LENGTH+CHECKSUM_LENGTH)));
	}

	private static void encodeData(BufferedImage image, Graphics2D g, byte[] binaryData, Position pos) {
		
		int bitsLeftInByte = BITS_IN_BYTE, currByteInd = 0, mask = BIT_GROUP_MASK_OF_ONES, ones_in_mask = ENCODING_BIT_GROUP_SIZE;
		byte currByte, currentData = 0;
		
		currByte = binaryData[currByteInd];
		
		while (true){
			if(ones_in_mask < bitsLeftInByte) { //mask doesn't cover all bits left in current byte
				currentData |= mask & currByte;
				currByte = (byte) (currByte >>> ones_in_mask);
				bitsLeftInByte-= ones_in_mask;
				mask = 0;
			}
			else {  //mask covers all bits left in current byte
				currentData |= (mask & currByte) << (ones_in_mask - bitsLeftInByte);
				mask = mask >>> bitsLeftInByte; //assuming ENCODING_COLOR_LEVELS is a power of 2!
				ones_in_mask-= bitsLeftInByte;
				bitsLeftInByte = 0;
			}
			
			if(mask == 0) { // encode block
				encodeBlock(currentData, g, pos);
				mask = BIT_GROUP_MASK_OF_ONES;
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;
				currentData = 0;
			}
			
			if(bitsLeftInByte == 0) {
				if(currByteInd+1<binaryData.length) {
					currByteInd++;
					currByte = binaryData[currByteInd];
					bitsLeftInByte = BITS_IN_BYTE;
				}
				else {
					encodeBlock(currentData, g, pos);
					return;
				}
			}
		}

	}
private static void encodeBlock(byte currentData, Graphics2D g, Position pos) {
	int maskedData = 0, level;
	Color color;
	
	//maskedData = maskDataBits(currentData);
	maskedData = (int) (currentData & 0x000000FF);
	level = (int) ((maskedData*GREY_SCALE_DELTA) & 0x000000FF);
	color = new Color(level, level, level);
	g.setColor(color);
	g.fillRect(pos.colModule * PIXELS_IN_MODULE, pos.rowModule * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
	pos.colModule++;		
	RotatedImageSampler.imageCheckForColumnEnd(pos, MODULES_IN_ENCODED_IMAGE_DIM);		
}


	private static void createPositionDetectors(BufferedImage image, Graphics2D g) {
		
		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;
		
		int rowTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopLeft = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int rowTopRight = MODULES_IN_MARGIN * PIXELS_IN_MODULE;
		int colTopRight = (MODULES_IN_ENCODED_IMAGE_DIM-
				MODULES_IN_MARGIN-MODULES_IN_POS_DET_DIM) * PIXELS_IN_MODULE;
		int rowBottomLeft = (MODULES_IN_ENCODED_IMAGE_DIM-
				MODULES_IN_MARGIN-MODULES_IN_POS_DET_DIM) * PIXELS_IN_MODULE;
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
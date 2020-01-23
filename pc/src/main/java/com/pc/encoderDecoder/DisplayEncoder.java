package com.pc.encoderDecoder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.pc.configuration.Parameters;

import static com.pc.configuration.Constants.*;


enum ROW {
	  WITH_POS_DET,
	  WITHOUT_POS_DET,
	  SAME_ROW
	}

public class DisplayEncoder {	

	public static BufferedImage encodeBytes(byte[] binaryData, byte[] IV, byte[] ivchecksum) throws Exception {
		//allocate space including white margins
		BufferedImage image = new BufferedImage(MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule,
				MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule, BufferedImage.TYPE_INT_ARGB);		 
		// Clear the background with white
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule, MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule);
		g.setColor(Color.BLACK);
		//create position detector
		createPositionDetectors(image, g);
		Position pos = new Position(Parameters.modulesInMargin, Parameters.modulesInMargin + MODULES_IN_POS_DET_DIM);
		encodeData(image,g, IV, pos); //encode IV first time
		encodeData(image,g, ivchecksum, pos); //encode IV checksum first time
		encodeData(image, g, binaryData, pos); 	//encode actual picture data
		encodeData(image,g, IV, pos); //encode IV second time
		encodeData(image,g, ivchecksum, pos); //encode IV checksum second time
		
		return image;
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
	g.fillRect(pos.colModule * Parameters.pixelsInModule, pos.rowModule * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
	pos.colModule++;		
	RotatedImageSampler.imageCheckForColumnEnd(pos, MODULES_IN_ENCODED_IMAGE_DIM);		
}


	private static void createPositionDetectors(BufferedImage image, Graphics2D g) {
		
		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;
		
		int rowTopLeft = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int colTopLeft = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int rowTopRight = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int colTopRight = (MODULES_IN_ENCODED_IMAGE_DIM-
				Parameters.modulesInMargin-MODULES_IN_POS_DET_DIM) * Parameters.pixelsInModule;
		int rowBottomLeft = (MODULES_IN_ENCODED_IMAGE_DIM-
				Parameters.modulesInMargin-MODULES_IN_POS_DET_DIM) * Parameters.pixelsInModule;
		int colBottomLeft = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int rowModuleOffset, colModuleOffset;
		
		for(rowModuleOffset = 0; rowModuleOffset< MODULES_IN_POS_DET_DIM; rowModuleOffset++) {
			for(colModuleOffset = 0; colModuleOffset< MODULES_IN_POS_DET_DIM; colModuleOffset++) {
				
				if( !((rowModuleOffset == MID_LAYER_OFFSET_1 || rowModuleOffset == MID_LAYER_OFFSET_2) &&
						(colModuleOffset >= MID_LAYER_OFFSET_1 && colModuleOffset <= MID_LAYER_OFFSET_2)) &&
						!((rowModuleOffset > MID_LAYER_OFFSET_1 && rowModuleOffset < MID_LAYER_OFFSET_2) &&
								(colModuleOffset == MID_LAYER_OFFSET_1 || colModuleOffset == MID_LAYER_OFFSET_2))) {
					g.fillRect(colTopLeft + colModuleOffset * Parameters.pixelsInModule,
							rowTopLeft + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
					g.fillRect(colTopRight + colModuleOffset * Parameters.pixelsInModule,
							 rowTopRight + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
					g.fillRect(colBottomLeft + colModuleOffset * Parameters.pixelsInModule,
							rowBottomLeft + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
				}
			}
		}		
	}	

}
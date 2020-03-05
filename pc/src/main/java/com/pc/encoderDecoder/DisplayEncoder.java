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
		createPositionDetectors(g);
		Position pos = new Position(Parameters.modulesInMargin, Parameters.modulesInMargin + MODULES_IN_POS_DET_DIM);
		encodeData(g, IV, pos, true); //encode IV first time
		encodeData(g, ivchecksum, pos, true); //encode IV checksum first time
		encodeData(g, binaryData, pos, false); 	//encode actual picture data
		encodeData(g, IV, pos, true); //encode IV second time
		encodeData(g, ivchecksum, pos, true); //encode IV checksum second time

		return image;
	}


	private static void encodeData(Graphics2D g, byte[] binaryData, Position pos, boolean isMetadata) {
		int nextElemStride, greenStride, blueStride;
		if(isMetadata) {
			nextElemStride = 1;	greenStride = 0; blueStride = 0;
		}
		else {
			nextElemStride = 3;	greenStride = 1; blueStride = 2;
		}

		int bitsLeftInByte = BITS_IN_BYTE, currByteInd = 0, mask = BIT_GROUP_MASK_OF_ONES,
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;

		byte currentDataR = 0, currentDataG = 0, currentDataB = 0;
		byte currByteR = binaryData[currByteInd];
		byte currByteG = binaryData[currByteInd+greenStride];
		byte currByteB = binaryData[currByteInd+blueStride];

		while (true){
			if(ones_in_mask < bitsLeftInByte) { //mask doesn't cover all bits left in current byte
				currentDataR |= mask & currByteR;
				currentDataG |= mask & currByteG;
				currentDataB |= mask & currByteB;
				currByteR = (byte) (currByteR >>> ones_in_mask);
				currByteG = (byte) (currByteG >>> ones_in_mask);
				currByteB = (byte) (currByteB >>> ones_in_mask);

				bitsLeftInByte-= ones_in_mask;
				mask = 0;
			}
			else {  //mask covers all bits left in current byte
				currentDataR |= (mask & currByteR) << (ones_in_mask - bitsLeftInByte);
				currentDataG |= (mask & currByteG) << (ones_in_mask - bitsLeftInByte);
				currentDataB |= (mask & currByteB) << (ones_in_mask - bitsLeftInByte);

				mask = mask >>> bitsLeftInByte; //assuming ENCODING_COLOR_LEVELS is a power of 2!
				ones_in_mask-= bitsLeftInByte;
				bitsLeftInByte = 0;
			}

			if(mask == 0) { // encode block
				encodeBlock(currentDataR, currentDataG, currentDataB, g, pos);
				mask = BIT_GROUP_MASK_OF_ONES;
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;
				currentDataR = 0; currentDataG = 0; currentDataB = 0;
			}

			if(bitsLeftInByte == 0) {
				if(currByteInd + nextElemStride < binaryData.length) {
					currByteInd+= nextElemStride;
					bitsLeftInByte = BITS_IN_BYTE;
					currByteR = binaryData[currByteInd];
					currByteG = binaryData[currByteInd+greenStride];
					currByteB = binaryData[currByteInd+blueStride];
				}
				else {
					encodeBlock(currentDataR, currentDataG, currentDataB, g, pos);
					return;
				}
			}
		}

	}
	private static void encodeBlock(byte currentDataR, byte currentDataG, byte currentDataB , Graphics2D g, Position pos) {
		int levelR, levelG, levelB;
		Color color;

		levelR = (currentDataR*GREY_SCALE_DELTA) & 0xFF;
		levelG = (currentDataG*GREY_SCALE_DELTA) & 0xFF;
		levelB = (currentDataB*GREY_SCALE_DELTA) & 0xFF;
		color = new Color(levelR, levelG, levelB);
		g.setColor(color);
		g.fillRect(pos.colModule * Parameters.pixelsInModule, pos.rowModule * Parameters.pixelsInModule,
                Parameters.pixelsInModule, Parameters.pixelsInModule);
		pos.colModule++;
		RotatedImageSampler.imageCheckForColumnEnd(pos, MODULES_IN_ENCODED_IMAGE_DIM);
	}


	private static void createPositionDetectors(Graphics2D g) {

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
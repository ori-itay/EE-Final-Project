package com.pc.encoderDecoder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import com.pc.Flow;
import com.pc.cli.EncodeDecodeCLI;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;

import static com.pc.configuration.Constants.*;

public class DisplayEncoder {
	static int encodedCnt;
	static boolean[][] positions;


	/**
	 * Encode byte array to 2-dim code (all elements of the encoding)
	 * @param binaryData - The byte array to be encoded
	 * @param dimsArr - The dimensions of the original image
	 * @param IV - The unique IV
	 * @param ivchecksum - Checksum for the IV
	 * @return The encoded image
	 */
	public static BufferedImage encodeBytes(byte[] binaryData, byte[] dimsArr, byte[] IV, byte[] ivchecksum) throws Exception {
		encodedCnt = 0;
		positions = new boolean[MODULES_IN_ENCODED_IMAGE_DIM][MODULES_IN_ENCODED_IMAGE_DIM];
		//allocate space including white margins
		BufferedImage image = new BufferedImage(MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule,
				MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule, BufferedImage.TYPE_INT_RGB);
		// Clear the background with white
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule, MODULES_IN_ENCODED_IMAGE_DIM*Parameters.pixelsInModule);
		//create position detector
		createPositionDetectors(g);
		g.setColor(Color.BLACK);
		createAlignmentPattern(g);
		Position pos = new Position(Parameters.modulesInMargin, Parameters.modulesInMargin + MODULES_IN_POS_DET_DIM);
		encodeData(g, IV, pos, true); //encode IV first time
		encodeData(g, ivchecksum, pos, true); //encode IV checksum first time
		encodeData(g, dimsArr, pos, true); //encode dims+checksum first time
		encodeData(g, binaryData, pos, false); 	//encode actual picture data
		encodeData(g, IV, pos, true); //encode IV second time
		encodeData(g, ivchecksum, pos, true); //encode IV checksum second time
		encodeData(g, dimsArr, pos, true); //encode dims+checksum second time
		pos.colModule = pos.rowModule = MODULES_IN_ENCODED_IMAGE_DIM - Parameters.modulesInMargin - 1;
		encodeBlock((byte) 0, (byte) 0, (byte) 0, g, pos); //last module will be black for corner detection
		return image;
	}

	/**
	 * Encode data section - byte array to modules. Encoding according to the position pos in the 2-dim matrix.
	 * metadata is encoded in all 3 RGB channels.
	 * @param g - Graphics2D instance for the encoding
	 * @param binaryData - The byte array to be encoded
	 * @param pos - Module position (X,Y encoding coordinates) of the encoding beginning.
	 * @param isMetadata - Indicates whether the data is metadata (IV/DIMS) - if so, data is encoded in all 3 RGB channels.
	 * @return void
	 */
	private static void encodeData(Graphics2D g, byte[] binaryData, Position pos, boolean isMetadata) {
		int nextElemStride, greenStride, blueStride, shift;
		if(isMetadata) {
			nextElemStride = 1;	greenStride = 0; blueStride = 0;
			shift = 0;
		}
		else {
			nextElemStride = 3;	greenStride = 1; blueStride = 2;
			shift = Parameters.colorDiscardedBits;
		}
		int bitsLeftInByte = BITS_IN_BYTE - shift, currByteInd = 0, mask = BIT_GROUP_MASK_OF_ONES,
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;

		byte currentDataR = 0, currentDataG = 0, currentDataB = 0;
		byte currByteR = (byte) (binaryData[currByteInd] >> shift);
		byte currByteG = (byte) (binaryData[currByteInd+greenStride] >> shift);
		byte currByteB = (byte) (binaryData[currByteInd+blueStride] >> shift);

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
					bitsLeftInByte = BITS_IN_BYTE - shift;
					currByteInd+= nextElemStride;

					if(currByteInd + blueStride < binaryData.length) {
						currByteR = (byte) (binaryData[currByteInd] >> shift);
						currByteG = (byte) (binaryData[currByteInd + greenStride] >> shift);
						currByteB = (byte) (binaryData[currByteInd + blueStride] >> shift);
					}
					else if (currByteInd + greenStride < binaryData.length) {
						currByteR = (byte) (binaryData[currByteInd] >> shift);
						currByteG = (byte) (binaryData[currByteInd + greenStride] >> shift);
						currByteB = 0;
					}
					else {
						currByteR = (byte) (binaryData[currByteInd] >> shift);
						currByteG = 0;
						currByteB = 0;
					}
				}
				else {
					if(mask != BIT_GROUP_MASK_OF_ONES)
						encodeBlock(currentDataR, currentDataG, currentDataB, g, pos);
					return;
				}
			}
		}

	}

	/**
	 * Encode block with certain color according to the delivered channels data.
	 * @param currentDataR - Data to encode in the Red channel
	 * @param currentDataG - Data to encode in the Green channel
	 * @param currentDataB - Data to encode in the Blue channel
	 * @param g - Graphics2D instance for the encoding
	 * @param pos - Position (X,Y encoding coordinates) of current block to be encoded
	 * @return void
	 */
	private static void encodeBlock(byte currentDataR, byte currentDataG, byte currentDataB , Graphics2D g, Position pos) {
		encodedCnt++;
		if(positions[pos.rowModule][pos.colModule]){
			//g.clearRect(pos.colModule * Parameters.pixelsInModule, pos.rowModule * Parameters.pixelsInModule,
			//		Parameters.pixelsInModule, Parameters.pixelsInModule);
			System.out.println("Encoding certain module twice - ERROR!\n");
			return;
		}
		positions[pos.rowModule][pos.colModule] = true;

		int levelR, levelG, levelB;
		Color color;

		levelR = (currentDataR*COLOR_SCALE_DELTA) & 0xFF;
		levelG = (currentDataG*COLOR_SCALE_DELTA) & 0xFF;
		levelB = (currentDataB*COLOR_SCALE_DELTA) & 0xFF;


		levelR = (int) (Math.round(Math.pow( ((double)levelR/255), Flow.gamma) * 255));
		if(levelR>255){ levelR = 255;}
		levelG = (int) (Math.round(Math.pow( ((double)levelG/255), Flow.gamma) * 255));
		if(levelG>255){ levelG = 255;}
		levelB = (int) (Math.round(Math.pow( ((double)levelB/255), Flow.gamma) * 255));
		if(levelB>255){ levelB = 255;}


		color = new Color(levelR, levelG, levelB);
		g.setColor(color);
		g.fillRect(pos.colModule * Parameters.pixelsInModule, pos.rowModule * Parameters.pixelsInModule,
                Parameters.pixelsInModule, Parameters.pixelsInModule);
		pos.colModule++;
		//skip alignment pattern
		if(pos.colModule == ALIGNMENT_PATTERN_UPPER_LEFT_MODULE &&
				(pos.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE &&
						pos.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM)){
			pos.colModule+=  MODULES_IN_ALIGNMENT_PATTERN_DIM;
		}
		RotatedImageSampler.imageCheckForColumnEnd(pos, MODULES_IN_ENCODED_IMAGE_DIM, Parameters.modulesInMargin);
	}

	/**
	 * Draw 3 QR position detectors in corners of the encoding region.
	 * @param g - Graphics2D instance for the encoding
	 * @return void
	 */
	private static void createPositionDetectors(Graphics2D g) {

		final int MID_LAYER_OFFSET_1 = 1; final int MID_LAYER_OFFSET_2 = 5;

		int rowTopLeft = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int colTopLeft = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int rowTopRight = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int colTopRight = (MODULES_IN_ENCODED_IMAGE_DIM-
				Parameters.modulesInMargin-MODULES_IN_POS_DET_DIM + 1) * Parameters.pixelsInModule;
		int rowBottomLeft = (MODULES_IN_ENCODED_IMAGE_DIM-
				Parameters.modulesInMargin-MODULES_IN_POS_DET_DIM + 1) * Parameters.pixelsInModule;
		int colBottomLeft = Parameters.modulesInMargin * Parameters.pixelsInModule;
		int rowModuleOffset, colModuleOffset;

		for(rowModuleOffset = 0; rowModuleOffset< MODULES_IN_POS_DET_DIM - 1; rowModuleOffset++) {
			for(colModuleOffset = 0; colModuleOffset< MODULES_IN_POS_DET_DIM - 1; colModuleOffset++) {

				if(rowModuleOffset == 0 || rowModuleOffset == MODULES_IN_POS_DET_DIM - 2
						|| colModuleOffset == 0 || colModuleOffset == MODULES_IN_POS_DET_DIM - 2){
					g.setColor(Color.BLACK);
					g.fillRect(colTopLeft + colModuleOffset * Parameters.pixelsInModule,
							rowTopLeft + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
					g.fillRect(colTopRight + colModuleOffset * Parameters.pixelsInModule,
							rowTopRight + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
					g.fillRect(colBottomLeft + colModuleOffset * Parameters.pixelsInModule,
							rowBottomLeft + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
				}
				else if(rowModuleOffset >= 2 && rowModuleOffset <= 4
						&& colModuleOffset >= 2 && colModuleOffset <= 4){
					g.setColor(Color.RED);
					g.fillRect(colTopLeft + colModuleOffset * Parameters.pixelsInModule,
							rowTopLeft + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
					g.setColor(Color.GREEN);
					g.fillRect(colTopRight + colModuleOffset * Parameters.pixelsInModule,
							rowTopRight + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
					g.setColor(Color.BLUE);
					g.fillRect(colBottomLeft + colModuleOffset * Parameters.pixelsInModule,
							rowBottomLeft + rowModuleOffset * Parameters.pixelsInModule, Parameters.pixelsInModule, Parameters.pixelsInModule);
				}
			}
		}
	}

	/**
	 * Draw alignment pattern withing a known constant distance from upper left corner.
	 * @param g - Graphics2D instance for the encoding
	 * @return void
	 */
	private static void createAlignmentPattern(Graphics2D g) {
		for(int rowModuleDiff = 0; rowModuleDiff < MODULES_IN_ALIGNMENT_PATTERN_DIM; rowModuleDiff++){
			for(int colModuleDiff = 0; colModuleDiff < MODULES_IN_ALIGNMENT_PATTERN_DIM; colModuleDiff++) {
				if(colModuleDiff > 0 && colModuleDiff < MODULES_IN_ALIGNMENT_PATTERN_DIM - 1 &&
						rowModuleDiff > 0 && rowModuleDiff < MODULES_IN_ALIGNMENT_PATTERN_DIM - 1 &&
						(rowModuleDiff != 2 || colModuleDiff != 2)){
					continue;
				}
				g.fillRect((ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + colModuleDiff) * Parameters.pixelsInModule,
						(ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + rowModuleDiff) * Parameters.pixelsInModule,
						Parameters.pixelsInModule, Parameters.pixelsInModule);
			}
		}
	}

}
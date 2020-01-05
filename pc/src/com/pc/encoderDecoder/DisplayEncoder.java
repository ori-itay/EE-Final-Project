package com.pc.encoderDecoder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import static com.pc.configuration.Constants.*;


enum ROW {
	  WITH_POS_DET,
	  WITHOUT_POS_DET,
	  SAME_ROW
	}

public class DisplayEncoder {
	

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
		encodeDataLen(image, g, binaryData.length(), pos); //in bytes
		//encode data - masked
		encodeData(image, g, binaryData, pos);
		
		return image;
	}


	private static void encodeData(BufferedImage image, Graphics2D g, String binaryData, Position pos) {
		
		int currentData = 0, maskedData = 0, bitsLeftInByte = BITS_IN_BYTE, currByteInd = 0, mask = BIT_GROUP_MASK_OF_ONES,
				ones_in_mask = ENCODING_BIT_GROUP_SIZE, level;
		byte currByte;
		Color color;
		
		byte[] stringAsBytes = binaryData.getBytes();
		
		currByte = stringAsBytes[currByteInd];
		
		while (true){
			if(ones_in_mask < bitsLeftInByte) { //mask doesn't cover all bits left in current byte
				currentData += mask & currByte;
				currByte = (byte) (currByte >>> ones_in_mask);
				bitsLeftInByte-= ones_in_mask;
				mask = 0;
			}
			else {  //mask covers all bits left in current byte
				currentData += (mask & currByte) << (ones_in_mask - bitsLeftInByte);
				mask = mask >>> bitsLeftInByte; //assuming ENCODING_COLOR_LEVELS is a power of 2!
				ones_in_mask-= bitsLeftInByte;
				bitsLeftInByte = 0;
			}
			
			if(mask == 0) { // encode block
				//maskedData = maskDataBits(currentData);
				maskedData = currentData;
				level = maskedData*GREY_SCALE_DELTA;
				color = new Color(level, level, level);
				g.setColor(color);
				g.fillRect(pos.colModule * PIXELS_IN_MODULE, pos.rowModule * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
				pos.colModule++;		
				RotatedImageSampler.checkForColumnEnd(pos);
				mask = BIT_GROUP_MASK_OF_ONES;
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;
				currentData = 0;
			}
			
			if(bitsLeftInByte == 0) {
				if(currByteInd+1<stringAsBytes.length) {
					currByteInd++;
					currByte = stringAsBytes[currByteInd];
					bitsLeftInByte = BITS_IN_BYTE;
				}
				else {
					//maskedData = maskDataBits(currentData);
					maskedData = currentData & 0xFF;
					level = maskedData*GREY_SCALE_DELTA;
					color = new Color(level, level, level);
					g.setColor(color);
					g.fillRect(pos.colModule * PIXELS_IN_MODULE, pos.rowModule * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
					pos.colModule++;		
					RotatedImageSampler.checkForColumnEnd(pos);
					return;
				}
			}
		}

	}

	private static void encodeDataLen(BufferedImage image, Graphics2D g, int length, Position pos) throws Exception {
		//LSB is encoded as the first (leftmost) module - little endian
		//data length is encoded in bytes
		int i, currentData, maskedData, level;
		Color color;
		
		pos.rowModule = MODULES_IN_MARGIN;		
		pos.colModule = MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM;
		
		
		if(length> MAX_ENCODED_LENGTH) {
			throw new Exception("Data len is: "+length+". file is too large to be encoded! Max legal len is: "+MAX_ENCODED_LENGTH);
		}
		
		for(i = 0; i < DATA_LEN_ENCODING_LENGTH_MODULES; i++) {
			currentData = BIT_GROUP_MASK_OF_ONES & length;
			//maskedBits = maskDataBits(currentData);
			maskedData = currentData;
			level = maskedData*GREY_SCALE_DELTA;
			color = new Color(level, level, level);
			g.setColor(color);
			g.fillRect(pos.colModule * PIXELS_IN_MODULE, pos.rowModule * PIXELS_IN_MODULE, PIXELS_IN_MODULE, PIXELS_IN_MODULE);
			pos.colModule++;		
			//maybe remove the following because this will probably wont be the situation
			RotatedImageSampler.checkForColumnEnd(pos);
			length = length>>>ENCODING_BIT_GROUP_SIZE;
		}
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
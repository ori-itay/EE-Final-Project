package com.pc.encoderDecoder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.pc.FlowUtils;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;

import static com.pc.configuration.Constants.*;


public class DisplayDecoder {

//	public static RotatedImageSampler decodeFilePC(String filepath) throws IOException {
//		File inputFile = new File(filepath);
//		BufferedImage encodedImage = ImageIO.read(inputFile);
//		int[][] pixelMatrix = convertTo2DUsingGetRGB(encodedImage);
//		RotatedImageSampler imageSampler = decodePixelMatrix(pixelMatrix);
//
//		//only temporary testing:
//		//CapturedImageSampler sampler = new CapturedImageSampler();
//		//sampler.locatePositionDetectors(filepath);
//
//		return imageSampler;
//	}

	public static void decodePixelMatrix(StdImageSampler imageSampler, int[][] pixelMatrix)  {

		configureImage(imageSampler, pixelMatrix);
		Position pos = new Position(imageSampler.getModulesInMargin(), imageSampler.getModulesInMargin() + MODULES_IN_POS_DET_DIM);
		imageSampler.setIV1(decodeData(imageSampler, Parameters.ivLength, pos, true));
		imageSampler.setIV1Checksum(decodeData(imageSampler, CHECKSUM_LENGTH, pos, true));
		imageSampler.setDimsAndChecksum1(decodeData(imageSampler, IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH, pos, true));
		int imageDataLength = FlowUtils.computeMaxEncodedLength(imageSampler.getModulesInDim(), imageSampler.getModulesInMargin());
		imageSampler.setDecodedData(decodeData(imageSampler, imageDataLength, pos, false));
		imageSampler.setIV2(decodeData(imageSampler, Parameters.ivLength, pos, true));
		imageSampler.setIV2Checksum(decodeData(imageSampler, CHECKSUM_LENGTH, pos, true));
		imageSampler.setDimsAndChecksum2(decodeData(imageSampler, IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH, pos, true));
		return;
	}

	private static void configureImage(StdImageSampler imageSampler, int[][] pixelMatrix) {
		imageSampler.setReceivedImageDim(pixelMatrix.length);
		imageSampler.setPixelMatrix(pixelMatrix);
		//configureModuleSizeAndRotation(imageSampler);
		assert( imageSampler.getModuleSize() != 0);
		//configureCrop(imageSampler);
		return;
	}


	private static byte[] decodeData(StdImageSampler imageSampler, int lengthInBytes, Position pos,
									 boolean isMetadata) {
		int nextElemStride, greenStride, blueStride;
		if(isMetadata) {
			nextElemStride = 1;	greenStride = 0; blueStride = 0;
		}
		else {
			nextElemStride = 3;	greenStride = 1; blueStride = 2;
		}

		byte[] decodedData = new byte[lengthInBytes];

		int bitsLeftToByte = BITS_IN_BYTE, currByteInd = 0, mask = BIT_GROUP_MASK_OF_ONES,
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;

		int[] RGB = sampleModule(imageSampler, pos, isMetadata);
		// assuming ENCODING_COLOR_LEVELS<255
		int RChannelValue = RGB[0]/ COLOR_SCALE_DELTA;
		int GChannelValue = RGB[1]/ COLOR_SCALE_DELTA;
		int BChannelValue = RGB[2]/ COLOR_SCALE_DELTA;
		byte currentDataR = 0, currentDataG = 0, currentDataB = 0;

		while (true){
			if(ones_in_mask < bitsLeftToByte) { //mask doesn't cover all bits left in current byte
				currentDataR += (0xFF) & ((mask & RChannelValue) << (BITS_IN_BYTE - bitsLeftToByte));
				currentDataG += (0xFF) & ((mask & GChannelValue) << (BITS_IN_BYTE - bitsLeftToByte));
				currentDataB += (0xFF) & ((mask & BChannelValue) << (BITS_IN_BYTE - bitsLeftToByte));
				bitsLeftToByte-= ones_in_mask;
				mask = 0;
			}
			else {  //mask covers all bits left in current byte
				currentDataR += ((0xFF) & ((mask & RChannelValue) >>> (ones_in_mask - bitsLeftToByte))) <<  (BITS_IN_BYTE - bitsLeftToByte);
				currentDataG += ((0xFF) & ((mask & GChannelValue) >>> (ones_in_mask - bitsLeftToByte))) <<  (BITS_IN_BYTE - bitsLeftToByte);
				currentDataB += ((0xFF) & ((mask & BChannelValue) >>> (ones_in_mask - bitsLeftToByte))) <<  (BITS_IN_BYTE - bitsLeftToByte);
				mask = mask >>> bitsLeftToByte; //assuming ENCODING_COLOR_LEVELS is a power of 2!
				ones_in_mask-= bitsLeftToByte;
				bitsLeftToByte = 0;
			}

			if(mask == 0 && bitsLeftToByte != 0) { // get next block
				mask = BIT_GROUP_MASK_OF_ONES;
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;
				RGB = sampleModule(imageSampler, pos, isMetadata);
				RChannelValue = (byte) (RGB[0]/ COLOR_SCALE_DELTA);
				GChannelValue = (byte) (RGB[1]/ COLOR_SCALE_DELTA);
				BChannelValue = (byte) (RGB[2]/ COLOR_SCALE_DELTA);
			}

			if(bitsLeftToByte == 0) {
				if(currByteInd+nextElemStride<lengthInBytes) {
					decodedData[currByteInd] = currentDataR;
					decodedData[currByteInd+greenStride] = currentDataG;
					decodedData[currByteInd+blueStride] = currentDataB;
					currByteInd+= nextElemStride;
					bitsLeftToByte = BITS_IN_BYTE;
					currentDataR = 0; currentDataG = 0; currentDataB = 0;
				}
				else{
					int remainder = lengthInBytes - currByteInd;
					switch (remainder){
						case(3): {
							decodedData[currByteInd+blueStride] = currentDataB;
						}
						case (2):{
							decodedData[currByteInd+greenStride] = currentDataG;
						}
						case(1):{
							decodedData[currByteInd] = currentDataR;
						}
					}/*
					if(currByteInd+blueStride<lengthInBytes){
						decodedData[currByteInd] = currentDataR;
						decodedData[currByteInd+greenStride] = currentDataG;
						decodedData[currByteInd+blueStride] = currentDataB;
					}
					if(currByteInd+greenStride<lengthInBytes){
						decodedData[currByteInd] = currentDataR;
						decodedData[currByteInd+greenStride] = currentDataG;
					}
					if(currByteInd<lengthInBytes){
						decodedData[currByteInd] = currentDataR;
					}*/
					return decodedData;
				}

			}
		}
	}

	private static int[] sampleModule(StdImageSampler imageSampler, Position pos, boolean duplicateChannels) {

		//int rowPixel = pos.rowModule * imageSampler.getModuleSize();
		//int colPixel = pos.colModule * imageSampler.getModuleSize();
		double rowPixel = (0.5 + pos.rowModule) * imageSampler.getModuleSize(); //  (imageSampler.getModuleSize() / 2) + (pos.rowModule * imageSampler.getModuleSize())
		double colPixel = (0.5 + pos.colModule) * imageSampler.getModuleSize();
		//int currPixelSample = imageSampler.getPixel(rowPixel, colPixel);
		int currPixelSample = imageSampler.getPixel(colPixel, rowPixel, duplicateChannels);
		int[] RGB = new int[3];

		RGB[0] = (currPixelSample) & 0xFF; //red
		RGB[1] = (currPixelSample >>> 8) & 0xFF; //green
		RGB[2] = (currPixelSample >>> 16) & 0xFF; //blue

		pos.colModule++;
		//skip alignment pattern
		if(pos.colModule == ALIGNMENT_PATTERN_UPPER_LEFT_MODULE && pos.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE
				&& pos.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM ){
			pos.colModule+=  MODULES_IN_ALIGNMENT_PATTERN_DIM;
		}
		imageSampler.checkForColumnEnd(pos);

		return RGB;
	}

	// packing an array of bytes to an int - Little Endian
	private static int byteArrayToInt(byte[] bytes) {
		int retVal = 0;
		for(int i = 0; i<bytes.length; i++)
			retVal |= ( (bytes[i] & 0xFF) << (i*BITS_IN_BYTE) );

		return retVal;
	}

	public static int TestByteArrayToInt(byte[] bytes) { return byteArrayToInt(bytes);}


//	private static void configureModuleSizeAndRotation(StdImageSampler imageSampler) {
//		int moduleSize;
//		int receivedDim = imageSampler.getReceivedImageDim();
//		int modulesInDim;
//
//		moduleSize = scanFromTopLeft(imageSampler);
//		if(moduleSize == 0) { //pattern not found from top left
//			moduleSize = scanFromBottomRight(imageSampler);
//
//			modulesInDim = Math.floorDiv(receivedDim,moduleSize);
//		}
//		else {
//			modulesInDim = Math.floorDiv(receivedDim,moduleSize);
//			//seek pattern from top right
//
//			if(!foundPattern(imageSampler.getPixelMatrix(), moduleSize, moduleSize * Parameters.modulesInMargin,
//					moduleSize * (modulesInDim -
//							Parameters.modulesInMargin - MODULES_IN_POS_DET_DIM) )) {
//
//			}
//			//seek pattern from top bottom left
//			else if(!foundPattern(imageSampler.getPixelMatrix(), moduleSize, moduleSize *
//					(modulesInDim -
//							Parameters.modulesInMargin - MODULES_IN_POS_DET_DIM) ,moduleSize * Parameters.modulesInMargin) ) {
//				imageSampler.rotationCounterClockwise = 90;
//			}
//			// else: pattern not found from bottom right - no need to rotate
//		}
//		imageSampler.setModuleSize(moduleSize);
//		imageSampler.setModulesInDim(modulesInDim);
//		return;
//	}

//	private static int scanFromBottomRight(StdImageSampler imageSampler) {
//		int[][] pixelMatrix = imageSampler.getPixelMatrix();
//
//		int offset = 1, firstBlack = pixelMatrix.length - 1, followingWhiteInd = 0, moduleSize = 0;
//
//		//search for first black pixel
//		while(firstBlack > pixelMatrix.length / 2) {
//			if(isBlackPixel(pixelMatrix[firstBlack][firstBlack])) {
//				while(isBlackPixel(pixelMatrix[firstBlack+1][firstBlack+1])) //found black pixel - go back until the very first one
//					firstBlack++;
//				break; // found first black pixel
//			}
//
//			firstBlack--;
//		}
//		//search for first following white pixel and extract module size (binary search)
//		offset = 1;
//		while(followingWhiteInd >=0) {
//			followingWhiteInd = firstBlack - offset;
//			while(followingWhiteInd < pixelMatrix.length && !isBlackPixel(pixelMatrix[followingWhiteInd][followingWhiteInd])) //found white pixel - go back until closest black pixel
//				followingWhiteInd++;
//			if(!isBlackPixel(pixelMatrix[followingWhiteInd-1][followingWhiteInd-1])) {  //found last black in module - extract module size
//				moduleSize = firstBlack - (followingWhiteInd-1);
//				break;
//			}
//			else {
//				offset*=2;
//			}
//		}
//		//search for position detector pattern
//		if(foundPattern(pixelMatrix, moduleSize,
//				firstBlack + 1 - moduleSize * MODULES_IN_POS_DET_DIM, firstBlack + 1 - moduleSize * MODULES_IN_POS_DET_DIM)) {
//			imageSampler.rotationCounterClockwise = 180;
//			imageSampler.setModulesInMargin(firstBlack/moduleSize);
//			return moduleSize;
//		}
//		return 0;
//	}
//
//	private static int scanFromTopLeft(StdImageSampler imageSampler) {
//		int[][] pixelMatrix = imageSampler.getPixelMatrix();
//		int offset = 1, firstBlack = 0, followingWhiteInd = 0 , moduleSize = 0;
//
//		//search for first black pixel
//		while(firstBlack < pixelMatrix.length / 2) {
//			if(isBlackPixel(pixelMatrix[firstBlack][firstBlack])) {
//				while(isBlackPixel(pixelMatrix[firstBlack-1][firstBlack-1])) //found black pixel - go back until the very first one
//					firstBlack--;
//				break; // found first black pixel
//			}
//			firstBlack++;
//		}
//		if(firstBlack>=pixelMatrix.length / 2) return 0; //black pixel not found in first half
//
//		//search for first following white pixel and extract module size (binary search)
//		offset = 1;
//		followingWhiteInd = firstBlack + offset;
//		while(followingWhiteInd < pixelMatrix.length) {
//			while(followingWhiteInd > 0 && !isBlackPixel(pixelMatrix[followingWhiteInd][followingWhiteInd])) ////found white pixel - go back until closest black pixel
//				followingWhiteInd--;
//			if(!isBlackPixel(pixelMatrix[followingWhiteInd+1][followingWhiteInd+1])) { //found last black in module - extract module size
//				moduleSize = followingWhiteInd+1-firstBlack;
//				break;
//			}
//			else {
//				offset*=2;
//			}
//			followingWhiteInd = firstBlack + offset;
//		}
//		//search for position detector pattern
//		if(foundPattern(pixelMatrix, moduleSize, firstBlack, firstBlack)) {
//			imageSampler.setModulesInMargin(firstBlack/moduleSize);
//			return moduleSize;
//		}
//		else{
//			imageSampler.rotationCounterClockwise = 270;
//		}
//		return 0;
//	}


	private static boolean foundPattern(int[][] pixelMatrix, int moduleSize, int rowStartPix, int colStartPix ) {
		if(	isBlackPixel(pixelMatrix[rowStartPix][colStartPix]) &&
				!isBlackPixel(pixelMatrix[rowStartPix+moduleSize][colStartPix+moduleSize]) &&
				isBlackPixel(pixelMatrix[rowStartPix+2*moduleSize][colStartPix+2*moduleSize]) &&
				isBlackPixel(pixelMatrix[rowStartPix+3*moduleSize][colStartPix+3*moduleSize]) &&
				isBlackPixel(pixelMatrix[rowStartPix+4*moduleSize][colStartPix+4*moduleSize]) &&
				!isBlackPixel(pixelMatrix[rowStartPix+5*moduleSize][colStartPix+5*moduleSize]) &&
				isBlackPixel(pixelMatrix[rowStartPix+6*moduleSize][colStartPix+6*moduleSize])	){
			return true;
		}
		return false;
	}


	private static boolean isBlackPixel(int pixel) {
		if (pixel == 0xFF000000){
			return true;
		}
		else {
			return false;
		}

	}


	private static int[][] convertTo2DUsingGetRGB(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[][] result = new int[height][width];

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				result[row][col] = image.getRGB(col, row);
			}
		}

		return result;
	}

}

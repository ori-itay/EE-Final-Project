package com.pc.encoderDecoder;

import com.pc.checksum.Checksum;
import com.pc.configuration.Parameters;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.pc.configuration.Constants.*;


public class DisplayDecoder {

	/**
	 * Increment position posT2 to the relevant coordinate for start of encoding after dataLengthBits/2 bits
	 * (first start of row after the relevant position for encoding half of the data length).
	 * @param imageSampler - Image sampler instance.
	 * @param posT2 - The position of the second "half" - to be incremented.
	 * @param dataLengthBits - The total length in bits of the current section.
	 * @return The exact number of bytes encoded by the first section ("half").
	 */
	private static int proceedPosToMidDataLength(StdImageSampler imageSampler, Position posT2, int dataLengthBits){
		int wantedLengthBits = dataLengthBits / 2;
		int totalBitsCovered = 0;
		int currLineLength;

		if(posT2.rowModule < MODULES_IN_POS_DET_DIM)
			currLineLength = imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM - posT2.colModule;
		else if(posT2.rowModule >= imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM)
			currLineLength = imageSampler.getModulesInDim() - posT2.colModule;
		else if(posT2.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin &&
				posT2.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM - Parameters.modulesInMargin)
			currLineLength = imageSampler.getModulesInDim() - posT2.colModule - MODULES_IN_ALIGNMENT_PATTERN_DIM;
		else
			currLineLength = imageSampler.getModulesInDim() - posT2.colModule;

		totalBitsCovered+= currLineLength*ENCODING_BIT_GROUP_SIZE*CHANNELS;
		posT2.rowModule++;

		while(totalBitsCovered < wantedLengthBits){
			if(posT2.rowModule < MODULES_IN_POS_DET_DIM){
				posT2.colModule = MODULES_IN_POS_DET_DIM;
				currLineLength = imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM - posT2.colModule;
			}
			else if(posT2.rowModule >= imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM){
				posT2.colModule = MODULES_IN_POS_DET_DIM;
				currLineLength = imageSampler.getModulesInDim() - posT2.colModule;
			}
			else if(posT2.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin &&
					posT2.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM - Parameters.modulesInMargin){
				posT2.colModule = 0;
				currLineLength = imageSampler.getModulesInDim() - posT2.colModule - MODULES_IN_ALIGNMENT_PATTERN_DIM;
			}
			else{
				posT2.colModule = 0;
				currLineLength = imageSampler.getModulesInDim() - posT2.colModule;
			}
			totalBitsCovered+= currLineLength*ENCODING_BIT_GROUP_SIZE*CHANNELS;
			posT2.rowModule++;
		}
		while( (totalBitsCovered % (CHANNELS*(BITS_IN_BYTE - Parameters.colorDiscardedBits))) != 0	){
			posT2.colModule++;
			//skip alignment pattern
			if(posT2.colModule == ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin &&
					posT2.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin
					&& posT2.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM - Parameters.modulesInMargin ){
				posT2.colModule+=  MODULES_IN_ALIGNMENT_PATTERN_DIM;
			}

			totalBitsCovered+= ENCODING_BIT_GROUP_SIZE*CHANNELS;
			imageSampler.imageCheckForColumnEnd(posT2, imageSampler.getModulesInDim(), 0);
		}

		return totalBitsCovered / (BITS_IN_BYTE - Parameters.colorDiscardedBits);
	}

	/**
	 * Increment position pos2 to the relevant coordinate after data area (right before IV2).
	 * @param imageSampler - Image sampler instance.
	 * @param pos2 - The position to be incremented.
	 * @param dataLengthBits - The total length in bits of the data section.
	 * @return void.
	 */
	private static void proceedPos2ToIV2(StdImageSampler imageSampler, Position pos2, int dataLengthBits){

		int totalBitsCovered = 0;
		int currLineLength;

		if(pos2.rowModule < MODULES_IN_POS_DET_DIM)
			currLineLength = imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM - pos2.colModule;
		else if(pos2.rowModule >= imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM)
			currLineLength = imageSampler.getModulesInDim() - pos2.colModule;
		else if(pos2.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin &&
				pos2.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM - Parameters.modulesInMargin)
			currLineLength = imageSampler.getModulesInDim() - pos2.colModule - MODULES_IN_ALIGNMENT_PATTERN_DIM;
		else
			currLineLength = imageSampler.getModulesInDim() - pos2.colModule;

		totalBitsCovered+= currLineLength*ENCODING_BIT_GROUP_SIZE*CHANNELS;
		pos2.rowModule++;

		while(totalBitsCovered < dataLengthBits){
			if(pos2.rowModule < MODULES_IN_POS_DET_DIM){
				pos2.colModule = MODULES_IN_POS_DET_DIM;
				currLineLength = imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM - pos2.colModule;
			}
			else if(pos2.rowModule >= imageSampler.getModulesInDim() - MODULES_IN_POS_DET_DIM){
				pos2.colModule = MODULES_IN_POS_DET_DIM;
				currLineLength = imageSampler.getModulesInDim() - pos2.colModule;
			}
			else if(pos2.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin &&
					pos2.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM - Parameters.modulesInMargin){
				pos2.colModule = 0;
				currLineLength = imageSampler.getModulesInDim() - pos2.colModule - MODULES_IN_ALIGNMENT_PATTERN_DIM;
			}
			else{
				pos2.colModule = 0;
				currLineLength = imageSampler.getModulesInDim() - pos2.colModule;
			}
			totalBitsCovered+= currLineLength*ENCODING_BIT_GROUP_SIZE*CHANNELS;
			pos2.rowModule++;
		}
		pos2.rowModule--;
		totalBitsCovered-= currLineLength*ENCODING_BIT_GROUP_SIZE*CHANNELS;

		while(totalBitsCovered < dataLengthBits){
			pos2.colModule++;
			//skip alignment pattern
			if(pos2.colModule == ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin &&
					pos2.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin
					&& pos2.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM - Parameters.modulesInMargin ){
				pos2.colModule+=  MODULES_IN_ALIGNMENT_PATTERN_DIM;
			}
			totalBitsCovered+= ENCODING_BIT_GROUP_SIZE*CHANNELS;
			imageSampler.imageCheckForColumnEnd(pos2, imageSampler.getModulesInDim(), 0);
		}
	}

	/**
	 * Convert byte array to int.
	 * @param bytes - The byte array.
	 * @param startIndex - The start index in the byte array.
	 * @param length - The number of bytes representing the integer in the byte array.
	 * @return The resulted integer.
	 */
	private static int getIntByteBuffer(byte[] bytes, int startIndex, int length) {
		short bytesShort = ByteBuffer.wrap(bytes, startIndex, length).getShort();
		return Short.toUnsignedInt(bytesShort);
	}

	/**
	 * Packing an array of bytes to an int - Little Endian.
	 * @param bytes - The byte array.
	 * @return The resulted integer.
	 */
	private static int byteArrayToInt(byte[] bytes) {
		int retVal = 0;
		for(int i = 0; i<bytes.length; i++)
			retVal |= ( (bytes[i] & 0xFF) << (i*BITS_IN_BYTE) );
		return retVal;
	}

	/**
	 * Checks whether the dimensions checksum is valid (compatible with the dimensions data).
	 * @param sampler - Image sampler instance.
	 * @param dimsAndChecksum - Byte array of the dimensions and checksum.
	 * @return True iff the checksum is valid.
	 */
	private static boolean isValidDimensionsChecksum(StdImageSampler sampler, byte[] dimsAndChecksum) {
		int width = getIntByteBuffer(dimsAndChecksum, 0, IMAGE_DIMENSION_ENCODING_LENGTH);
		int height = getIntByteBuffer(dimsAndChecksum,
				IMAGE_DIMENSION_ENCODING_LENGTH, IMAGE_DIMENSION_ENCODING_LENGTH);
		byte[] dimensionsChecksum = ByteBuffer.wrap(Arrays.copyOfRange(dimsAndChecksum
				, 2 * IMAGE_DIMENSION_ENCODING_LENGTH, 2 * IMAGE_DIMENSION_ENCODING_LENGTH + CHECKSUM_LENGTH)).array();

		boolean validDimensions = Checksum.isValidChecksum(width, height, dimensionsChecksum);
		if (validDimensions) {
			sampler.setWidth(width);
			sampler.setHeight(height);
			return true;
		}
		return false;
	}

	/**
	 * Decode the data in the delivered pixel matrix - decoded data inserted to image sampler fields.
	 * 4 threads are invoked to process the data section simultaneously.
	 * @param imageSampler - Image sampler instance.
	 * @param pixelMatrix - 2-D array(matrix) of the encoding pixels.
	 * @return True if decode process succeeded, False otherwise.
	 */
	public static boolean decodePixelMatrix(StdImageSampler imageSampler, int[][] pixelMatrix) throws InterruptedException {
		configureImage(imageSampler, pixelMatrix);
		Position pos = new Position(imageSampler.getModulesInMargin(), imageSampler.getModulesInMargin() + MODULES_IN_POS_DET_DIM);
		int imageDataLength = computeMaxEncodedLength(imageSampler.getModulesInDim());

		imageSampler.setIV1(decodeData(imageSampler, Parameters.ivLength, pos, true));
		imageSampler.setIV1Checksum(decodeData(imageSampler, CHECKSUM_LENGTH, pos, true));
		imageSampler.setDimsAndChecksum1(decodeData(imageSampler, IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH, pos, true));
		boolean iv1, dims1;

		dims1 = isValidDimensionsChecksum(imageSampler, imageSampler.getDimsAndChecksum1());
		iv1 = Checksum.isValidChecksum(imageSampler.getIV1Checksum(), imageSampler.getIV1());

		if (!iv1 || !dims1) {
			Position pos2 = new Position(pos.rowModule,pos.colModule);
			proceedPos2ToIV2(imageSampler, pos2, imageDataLength*(BITS_IN_BYTE - Parameters.colorDiscardedBits));
			imageSampler.setIV2(decodeData(imageSampler, Parameters.ivLength, pos2, true));
			imageSampler.setIV2Checksum(decodeData(imageSampler, CHECKSUM_LENGTH, pos2, true));
			imageSampler.setDimsAndChecksum2(decodeData(imageSampler, IMAGE_DIMS_ENCODING_LENGTH + CHECKSUM_LENGTH, pos2, true));
		}

		if ((!dims1 && !isValidDimensionsChecksum(imageSampler, imageSampler.getDimsAndChecksum2())) || (!iv1 && !Checksum.isValidChecksum(imageSampler.getIV2Checksum(), imageSampler.getIV2()))) {
			return false;
		}

		if (!iv1) {
			imageSampler.setIV(imageSampler.getIV2());
		}
		else {
			imageSampler.setIV(imageSampler.getIV1()); // correct iv and dims are in sampler
		}

		Position posT1 = new Position(pos.rowModule,pos.colModule);
		int FirstHalfDataLength = proceedPosToMidDataLength(imageSampler, pos, imageDataLength*(BITS_IN_BYTE - Parameters.colorDiscardedBits));
		int SecondHalfDataLength = imageDataLength - FirstHalfDataLength;
		Position posT2 = new Position(posT1.rowModule,posT1.colModule); //These are the OLD pos coordinates!
		int T1DataLength = proceedPosToMidDataLength(imageSampler, posT2, FirstHalfDataLength*(BITS_IN_BYTE - Parameters.colorDiscardedBits));
		int T2DataLength = FirstHalfDataLength - T1DataLength;
		Position posT3 = new Position(pos.rowModule,pos.colModule); //These are the NEW pos coordinates!
		int T3DataLength = proceedPosToMidDataLength(imageSampler, pos, SecondHalfDataLength*(BITS_IN_BYTE - Parameters.colorDiscardedBits));
		int T4DataLength = SecondHalfDataLength - T3DataLength;

		byte[] decodedData =  new byte[imageDataLength];

		Thread t1 = new Thread(()->{
			System.arraycopy(decodeData(imageSampler, T1DataLength, posT1, false), 0, decodedData,0, T1DataLength);
		});
		Thread t2 = new Thread(()->{
			System.arraycopy(decodeData(imageSampler, T2DataLength, posT2, false), 0, decodedData, T1DataLength, T2DataLength);

		});
		Thread t3 = new Thread(()->{
			System.arraycopy(decodeData(imageSampler, T3DataLength, posT3, false), 0, decodedData, FirstHalfDataLength, T3DataLength);

		});
		Thread t4 = new Thread(()->{
			System.arraycopy(decodeData(imageSampler, T4DataLength, pos, false), 0, decodedData,
					FirstHalfDataLength + T3DataLength, T4DataLength);

		});

		t1.start();	t2.start();	t3.start();	t4.start();
		t1.join(); t2.join(); t3.join(); t4.join();
		imageSampler.setDecodedData(decodedData);
		return true;
	}

	/**
	 * Set image dims and the pixel matrix to the sampler.
	 * @param imageSampler - Image sampler instance.
	 * @param pixelMatrix - 2-D array(matrix) of the encoding pixels.
	 * @return void.
	 */
	private static void configureImage(StdImageSampler imageSampler, int[][] pixelMatrix) {
		imageSampler.setReceivedImageDim(pixelMatrix.length);
		imageSampler.setPixelMatrix(pixelMatrix);
		assert( imageSampler.getModuleSize() != 0);
		return;
	}

	/**
	 * Decode data section in the relevant length, starting from position pos.
	 * @param imageSampler - Image sampler instance.
	 * @param lengthInBytes - Total length in bytes to be encoded.
	 * @param pos - The module position (X,Y coordinates) in the encoded image to start the decoding from.
	 * @param isMetadata - Indicates whether the data is metadata (IV/DIMS) - if so, data is decoded from all 3 RGB channels.
	 * @return The decoded data byte array.
	 */
	private static byte[] decodeData(StdImageSampler imageSampler, int lengthInBytes, Position pos, boolean isMetadata) {
		int nextElemStride, greenStride, blueStride, shift;
		if(isMetadata) {
			nextElemStride = 1;	greenStride = 0; blueStride = 0;
			shift = 0;
		}
		else {
			nextElemStride = 3;	greenStride = 1; blueStride = 2;
			shift = Parameters.colorDiscardedBits;
		}

		byte[] decodedData = new byte[lengthInBytes];

		int bitsLeftToByte = BITS_IN_BYTE - shift, currByteInd = 0, mask = BIT_GROUP_MASK_OF_ONES,
				ones_in_mask = ENCODING_BIT_GROUP_SIZE;
		int[] RGB = sampleModule(imageSampler, pos, isMetadata);
		// assuming ENCODING_COLOR_LEVELS<255
		int RChannelValue = RGB[0]/COLOR_SCALE_DELTA;
		int GChannelValue = RGB[1]/COLOR_SCALE_DELTA;
		int BChannelValue = RGB[2]/COLOR_SCALE_DELTA;
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
				RChannelValue = RGB[0]/COLOR_SCALE_DELTA;
				GChannelValue = RGB[1]/COLOR_SCALE_DELTA;
				BChannelValue = RGB[2]/COLOR_SCALE_DELTA;
			}

			if(bitsLeftToByte == 0) {
				if(currByteInd+nextElemStride<lengthInBytes) {
					decodedData[currByteInd] = currentDataR;
					decodedData[currByteInd+greenStride] = currentDataG;
					decodedData[currByteInd+blueStride] = currentDataB;
					currByteInd+= nextElemStride;
					bitsLeftToByte = BITS_IN_BYTE - shift;
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
					}
					return decodedData;
				}
			}
		}
	}

	/**
	 * Receive the color values of the current module.
	 * @param imageSampler - Image sampler instance.
	 * @param pos - The module position (X,Y coordinates) in the encoded image to start the decoding from.
	 * @param duplicateChannels - Indicates whether the data is metadata (IV/DIMS) - if so, value is determined by majority of the channels.
	 * @return int array of the R,G,B pixel values.
	 */
	private static int[] sampleModule(StdImageSampler imageSampler, Position pos, boolean duplicateChannels) {
		double rowPixel = (0.5 + pos.rowModule) * imageSampler.getModuleSize(); //  (imageSampler.getModuleSize() / 2) + (pos.rowModule * imageSampler.getModuleSize())
		double colPixel = (0.5 + pos.colModule) * imageSampler.getModuleSize();
		int currPixelSample = imageSampler.getPixel(colPixel, rowPixel, duplicateChannels, true);
		int[] RGB = new int[3];

		RGB[0] = (currPixelSample) & 0xFF; //red
		RGB[1] = (currPixelSample >>> 8) & 0xFF; //green
		RGB[2] = (currPixelSample >>> 16) & 0xFF; //blue

		pos.colModule++;
		//skip alignment pattern
		if(pos.colModule == ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin &&
				pos.rowModule >= ALIGNMENT_PATTERN_UPPER_LEFT_MODULE - Parameters.modulesInMargin
				&& pos.rowModule < ALIGNMENT_PATTERN_UPPER_LEFT_MODULE + MODULES_IN_ALIGNMENT_PATTERN_DIM - Parameters.modulesInMargin ){
			pos.colModule+=  MODULES_IN_ALIGNMENT_PATTERN_DIM;
		}
		imageSampler.checkForColumnEnd(pos);

		return RGB;
	}

	/**
	 * Compute the maximum number of bytes that can be encoded in an 2-D encoding with "dim" dimension (number of modules in axis).
	 * @param dim - The number of modules in dimension (X or Y axis).
	 * @return The maximum number of bytes that can be encoded.
	 */
	private static int computeMaxEncodedLength(int dim) {
		//metadata (i.e iv+checksum + dims+checksum) is encoded "three times" - once in each channel (RGB)
		int modulesForAlignmentPattern = 0;
		if(dim >= MODULES_FROM_UPPER_LEFT_TO_ALIGNMENT_BOTTOM_RIGHT){
			modulesForAlignmentPattern = MODULES_IN_ALIGNMENT_PATTERN_DIM*MODULES_IN_ALIGNMENT_PATTERN_DIM;
		}
		int modulesForPosDet = MODULES_IN_POS_DET_DIM*MODULES_IN_POS_DET_DIM*NUM_OF_POSITION_DETECTORS;
		int modulesForIVChecksum =  2*(int)(( (BITS_IN_BYTE*CHECKSUM_LENGTH + ENCODING_BIT_GROUP_SIZE - 1)/(double)ENCODING_BIT_GROUP_SIZE));
		int modulesForIV = 2*(int)(( (BITS_IN_BYTE*Parameters.ivLength + ENCODING_BIT_GROUP_SIZE - 1)/(double)ENCODING_BIT_GROUP_SIZE));
		int modulesForDims = 2*(int)( ((BITS_IN_BYTE*(IMAGE_DIMS_ENCODING_LENGTH+CHECKSUM_LENGTH) + ENCODING_BIT_GROUP_SIZE - 1)
				/(double)ENCODING_BIT_GROUP_SIZE));
		int modulesForMetadata = modulesForIVChecksum + modulesForIV + modulesForDims;
		int modulesForRightLowerCorner = 1;

		int modulesForEncoding = dim*dim  - (modulesForMetadata + modulesForPosDet + modulesForAlignmentPattern + modulesForRightLowerCorner);
		int maxBitsToEncode = ENCODING_BIT_GROUP_SIZE*modulesForEncoding;
		int maxBytesToEncode = CHANNELS*(maxBitsToEncode/(BITS_IN_BYTE - Parameters.colorDiscardedBits));
		return maxBytesToEncode;
	}

}
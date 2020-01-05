package com.pc.encoderDecoder;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import static com.pc.configuration.Constants.*;

public class DisplayDecoder {
	
	public static RotatedImageSampler decodeFilePC(File inputFile) throws Exception { //change here to receive pixelMatrix
		
		BufferedImage encodedImage = ImageIO.read(inputFile);
		int[][] pixelMatrix = convertTo2DUsingGetRGB(encodedImage);
		RotatedImageSampler imageSampler = decodePixelMatrix(pixelMatrix);
		
		return imageSampler;
	}
	
	public static RotatedImageSampler decodePixelMatrix(int[][] pixelMatrix) throws Exception { //change here to receive pixelMatrix
					
		Position pos = new Position(MODULES_IN_MARGIN, MODULES_IN_MARGIN + MODULES_IN_POS_DET_DIM);
		//extract image configuration and return cropped rotated image
		
		RotatedImageSampler imageSampler = configureImage(pixelMatrix, pos);
		//decode image data to byte array
		imageSampler.decodedData = decodeConfiguredImage(imageSampler, imageSampler.dataLength, pos);

		return imageSampler;
	}

	private static RotatedImageSampler configureImage(int[][] pixelMatrix, Position pos) {
		RotatedImageSampler imageSampler = new RotatedImageSampler();
		imageSampler.pixelMatrix = pixelMatrix;
		configureModuleSizeAndRotation(imageSampler);
		assert( imageSampler.moduleSize != 0);	
		//configureCrop(imageSampler);
		byte[] dataLengthBytes = decodeConfiguredImage(imageSampler, DATA_LEN_ENCODING_LENGTH_MODULES, pos);
		imageSampler.dataLength = byteArrayToInt(dataLengthBytes);
		
		return imageSampler;
	}
	
	 // packing an array of bytes to an int - Little Endian
	private static int byteArrayToInt(byte[] bytes) {
		int retVal = 0;
		for(int i = 0; i<bytes.length; i++) 
		     retVal |= ( (bytes[i] & 0xFF) << (i*BITS_IN_BYTE) );
		
		return retVal;
	}
	
	public static int TestByteArrayToInt(byte[] bytes) { return byteArrayToInt(bytes);}


	private static void configureModuleSizeAndRotation(RotatedImageSampler imageSampler) {
		int moduleSize;
    		
		moduleSize = scanFromTopLeft(imageSampler.pixelMatrix);
		if(moduleSize == 0) { //pattern not found from top left
			moduleSize = scanFromBottomRight(imageSampler.pixelMatrix);
			imageSampler.rotationCounterClockwise = 180;
		}
		else {
			//seek pattern from top right
			if(!foundPattern(imageSampler.pixelMatrix, moduleSize, moduleSize * MODULES_IN_MARGIN,
					moduleSize * (MODULES_IN_ENCODED_IMAGE_DIM - 
							MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) )) {
				imageSampler.rotationCounterClockwise = 270;
			}
			//seek pattern from top bottom left
			else if(!foundPattern(imageSampler.pixelMatrix, moduleSize, moduleSize *
					(MODULES_IN_ENCODED_IMAGE_DIM - 
							MODULES_IN_MARGIN - MODULES_IN_POS_DET_DIM) ,moduleSize * MODULES_IN_MARGIN) ) {
				imageSampler.rotationCounterClockwise = 90;
			}
			// else: pattern not found from bottom right - no need to rotate
		}
		imageSampler.moduleSize = moduleSize;
		return;
	}

	private static int scanFromBottomRight(int[][] pixelMatrix) {
		
		int offset = 1, start = pixelMatrix.length - 1, firstBlack = start, followingWhiteInd = 0, moduleSize = 0;
	
		//search for first black pixel
		while(firstBlack > pixelMatrix.length / 2) {
			if(isBlackPixel(pixelMatrix[firstBlack][firstBlack])) {
				while(isBlackPixel(pixelMatrix[firstBlack+1][firstBlack+1])) //found black pixel - go back until the very first one
					firstBlack++;
				break; // found first black pixel
			}
				
			firstBlack = start - offset;
			offset*=2;
		}	
		//search for first following white pixel and extract module size (binary search)
		offset = 1;
		while(followingWhiteInd >=0) {
			followingWhiteInd = firstBlack - offset;
			while(followingWhiteInd < pixelMatrix.length && !isBlackPixel(pixelMatrix[followingWhiteInd][followingWhiteInd])) //found white pixel - go back until closest black pixel
				followingWhiteInd++;
			if(!isBlackPixel(pixelMatrix[followingWhiteInd-1][followingWhiteInd-1])) {  //found last black in module - extract module size 
				moduleSize = firstBlack - (followingWhiteInd-1);
				break;
			}
			else {
				offset*=2;
			}
		}
		//search for position detector pattern
		if(foundPattern(pixelMatrix, moduleSize, 
				firstBlack + 1 - moduleSize * MODULES_IN_POS_DET_DIM, firstBlack + 1 - moduleSize * MODULES_IN_POS_DET_DIM)) {
			return moduleSize;
		}
		return 0;
	}

	private static int scanFromTopLeft(int[][] pixelMatrix) {
		
		int offset = 1, start = 0, firstBlack = start, followingWhiteInd = 0 , moduleSize = 0;
	
		//search for first black pixel
		while(firstBlack < pixelMatrix.length / 2) {
			if(isBlackPixel(pixelMatrix[firstBlack][firstBlack])) {
				while(isBlackPixel(pixelMatrix[firstBlack-1][firstBlack-1])) //found black pixel - go back until the very first one
					firstBlack--;
				break; // found first black pixel
			}
			firstBlack = start + offset;
			offset*=2;
		}
		if(firstBlack>=pixelMatrix.length / 2) return 0; //black pixel not found in first half
		
		//search for first following white pixel and extract module size (binary search)
		offset = 1;
		followingWhiteInd = firstBlack + offset;
		while(followingWhiteInd < pixelMatrix.length) {
			while(followingWhiteInd > 0 && !isBlackPixel(pixelMatrix[followingWhiteInd][followingWhiteInd])) ////found white pixel - go back until closest black pixel
				followingWhiteInd--;
			if(!isBlackPixel(pixelMatrix[followingWhiteInd+1][followingWhiteInd+1])) { //found last black in module - extract module size 
				moduleSize = followingWhiteInd+1-firstBlack;
				break;
			}
			else {
				offset*=2;
			}
			followingWhiteInd = firstBlack + offset;
		}
		//search for position detector pattern
		if(foundPattern(pixelMatrix, moduleSize, firstBlack, firstBlack)) {
			return moduleSize;
		}
		return 0;
	}


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



	private static byte[] decodeConfiguredImage(RotatedImageSampler imageSampler, int bitLength, Position pos) {
		
		int byteLength = (int) Math.ceil((bitLength+0.0)/BITS_IN_BYTE);
		byte[] decodedData = new byte[byteLength];
		
		for(int bitIndex = 0; bitIndex<bitLength; bitIndex++) {
			decodeModule(decodedData, imageSampler, pos, bitIndex/BITS_IN_BYTE, bitIndex%BITS_IN_BYTE);
			pos.colModule++;
			RotatedImageSampler.checkForColumnEnd(pos);
			
		}
		return decodedData;	
	}

	private static void decodeModule(byte[] decodedData, RotatedImageSampler imageSampler, Position pos, int dataIndex, int byteModulus) {
		
		boolean toggleBit, isBlackPixel;
		int rowPixel = pos.rowModule * imageSampler.moduleSize;
		int colPixel = pos.colModule * imageSampler.moduleSize; 
		
		toggleBit = (pos.colModule%3) == 0;	
		isBlackPixel = isBlackPixel(imageSampler.getPixel(rowPixel, colPixel));//change here to imageSampler
		
		if(isBlackPixel ^ toggleBit)
			decodedData[dataIndex]|= (1<<byteModulus);
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

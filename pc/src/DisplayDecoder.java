import java.awt.image.BufferedImage;

public class DisplayDecoder {
	
	
	public static void main(String... args) throws Exception {
		//String testData = "BLABLA";
		String testData = "BLABLAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
		BufferedImage encodedImage = DisplayEncoder.encodeBytes(testData);
		String decodedString = decodeImage(encodedImage);
		//print data for testing
		System.out.println(decodedString);
	}
	
	public static String decodeImage(BufferedImage encodedImage) throws Exception {
			
		Position pos = new Position();
		pos.rowModule = RotatedImageSampler.MODULES_IN_MARGIN;
		pos.colModule = RotatedImageSampler.MODULES_IN_MARGIN + RotatedImageSampler.MODULES_IN_POS_DET_DIM;
		//extract image configuration and return cropped rotated image
		RotatedImageSampler qrImage = configureImage(encodedImage, pos);
		//decode image data to byte array
		byte[] decodedData = decodeConfiguredImage(qrImage, qrImage.dataLength, pos);

		return new String(decodedData);
	}

	private static RotatedImageSampler configureImage(BufferedImage encodedImage, Position pos) {
		RotatedImageSampler qrImage = new RotatedImageSampler();
		qrImage.pixelMatrix = convertTo2DUsingGetRGB(encodedImage);
		configureModuleSizeAndRotation(qrImage);
		assert( qrImage.moduleSize != 0);	
		//configureCrop(qrImage);
		byte[] dataLengthBytes = decodeConfiguredImage(qrImage, RotatedImageSampler.DATA_LEN_ENCODING_LENGTH, pos);
		qrImage.dataLength = byteArrayToInt(dataLengthBytes);
		
		return qrImage;
	}
	
	 // packing an array of bytes to an int - Little Endian
	static int byteArrayToInt(byte[] bytes) {
		int retVal = 0;
		for(int i = 0; i<bytes.length; i++) 
		     retVal |= ( (bytes[i] & 0xFF) << (i*RotatedImageSampler.BITS_IN_BYTE) );
		
		return retVal;
	}


	private static void configureModuleSizeAndRotation(RotatedImageSampler qrImage) {
		int moduleSize;
    		
		moduleSize = scanFromTopLeft(qrImage.pixelMatrix);
		if(moduleSize == 0) { //pattern not found from top left
			moduleSize = scanFromBottomRight(qrImage.pixelMatrix);
			qrImage.rotationCounterClockwise = 180;
		}
		else {
			//seek pattern from top right
			if(!foundPattern(qrImage.pixelMatrix, moduleSize, moduleSize * ImageSampler.MODULES_IN_MARGIN,
					moduleSize * (ImageSampler.MODULES_IN_ENCODED_IMAGE_DIM - 
							ImageSampler.MODULES_IN_MARGIN - ImageSampler.MODULES_IN_POS_DET_DIM) )) {
				qrImage.rotationCounterClockwise = 270;
			}
			//seek pattern from top bottom left
			else if(!foundPattern(qrImage.pixelMatrix, moduleSize, moduleSize *
					(ImageSampler.MODULES_IN_ENCODED_IMAGE_DIM - 
							ImageSampler.MODULES_IN_MARGIN - ImageSampler.MODULES_IN_POS_DET_DIM) ,moduleSize * ImageSampler.MODULES_IN_MARGIN) ) {
				qrImage.rotationCounterClockwise = 90;
			}
			// else: pattern not found from bottom right - no need to rotate
		}
		qrImage.moduleSize = moduleSize;
		return;
	}

private static int scanFromBottomRight(int[][] pixelMatrix) {
	
	int offset = 1, start = pixelMatrix[0].length, ind = 0, moduleSize = 0;

	while(start > pixelMatrix.length / 2) {
		if(isBlackPixel(pixelMatrix[start][start])) 
			break;
		start--;
	}
	ind = start - offset;
	while(ind < pixelMatrix.length) {
		while(!isBlackPixel(pixelMatrix[ind][ind])) 
			ind++;
		if(!isBlackPixel(pixelMatrix[ind+1][ind+1])) { 
			moduleSize = start - (ind+1);
			break;
		}
		else {
			offset*=2;
		}
		ind = start - offset;
	}
	
	if(foundPattern(pixelMatrix, moduleSize, 
			start - moduleSize * ImageSampler.MODULES_IN_POS_DET_DIM, start - moduleSize * ImageSampler.MODULES_IN_POS_DET_DIM)) {
		return moduleSize;
	}
	return 0;
}

private static int scanFromTopLeft(int[][] pixelMatrix) {
	
	int offset = 1, start = 0, ind = 0, moduleSize = 0;

	while(start < pixelMatrix.length / 2) {
		if(isBlackPixel(pixelMatrix[start][start])) 
			break;
		start++;
	}
	ind = start + offset;
	while(ind < pixelMatrix.length) {
		while(!isBlackPixel(pixelMatrix[ind][ind])) 
			ind--;
		if(!isBlackPixel(pixelMatrix[ind+1][ind+1])) { 
			moduleSize = ind+1-start;
			break;
		}
		else {
			offset*=2;
		}
		ind = start + offset;
	}
	
	if(foundPattern(pixelMatrix, moduleSize, start, start)) {
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


//  following code is using Zxing https://stackoverflow.com/questions/27823628/get-the-coordinate-of-alignment-pattern-with-zxing 
//	private Result readQRCode(BufferedImage bi){
//	    BinaryBitmap binaryBitmap;
//	    Result result;
//	    try{
//	        binaryBitmap = new BinaryBitmap( new HybridBinarizer(new BufferedImageLuminanceSource( bi )));
//	        result = new QRCodeReader().decode(binaryBitmap);        
//	        return result;
//	    }catch(Exception ex){
//	        ex.printStackTrace();
//	        return null;
//	    }                
//	}

	private static byte[] decodeConfiguredImage(RotatedImageSampler qrImage, int bitLength, Position pos) {
		
		int byteLength = (int) Math.ceil((bitLength+0.0)/ImageSampler.BITS_IN_BYTE);
		byte[] decodedData = new byte[byteLength];
		
		for(int bitIndex = 0; bitIndex<bitLength; bitIndex++) {
			decodeModule(decodedData, qrImage, pos, bitIndex/ImageSampler.BITS_IN_BYTE, bitIndex%ImageSampler.BITS_IN_BYTE);
			pos.colModule++;
			RotatedImageSampler.checkForColumnEnd(pos);
			
		}
		return decodedData;	
	}

	private static void decodeModule(byte[] decodedData, RotatedImageSampler qrImage, Position pos, int dataIndex, int byteModulus) {
		
		boolean toggleBit, isBlackPixel;
		int rowPixel = pos.rowModule * qrImage.moduleSize;
		int colPixel = pos.colModule * qrImage.moduleSize; 
		
		toggleBit = (pos.colModule%3) == 0;	
		isBlackPixel = isBlackPixel(qrImage.getPixel(rowPixel, colPixel));//change here to imageSampler
		
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

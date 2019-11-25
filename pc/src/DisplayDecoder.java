<<<<<<< HEAD:pc/src/DisplayDecoder.java
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
		pos.rowModule = QrImage.MODULES_IN_MARGIN;
		pos.colModule = QrImage.MODULES_IN_MARGIN + QrImage.MODULES_IN_POS_DET_DIM;
		//extract image configuration and return cropped rotated image
		QrImage qrImage = configureImage(encodedImage, pos);
		//decode image data to byte array
		byte[] decodedData = decodeConfiguredImage(qrImage, qrImage.dataLength, pos);

		return new String(decodedData);
	}

	private static QrImage configureImage(BufferedImage encodedImage, Position pos) {
		QrImage qrImage = new QrImage();
		qrImage.pixelMatrix = convertTo2DUsingGetRGB(encodedImage);
		qrImage.moduleSize = extractModuleSize(qrImage.pixelMatrix);
		assert( qrImage.moduleSize != 0);
		//cropAndRotate(qrImage);		
		byte[] dataLengthBytes = decodeConfiguredImage(qrImage, QrImage.DATA_LEN_ENCODING_LENGTH, pos);
		qrImage.dataLength = fromByteArray(dataLengthBytes);
		
		return qrImage;
	}
	
	 // packing an array of bytes to an int - Little Endian
	static int fromByteArray(byte[] bytes) {
		int retVal = 0;
		for(int i = 0; i<bytes.length; i++) 
		     retVal |= ( (bytes[i] & 0xFF) << (i*QrImage.BITS_IN_BYTE) );
		
		return retVal;
//	     return ((bytes[2] & 0xFF) << 16) | 
//	            ((bytes[1] & 0xFF) << 8 ) | 
//	            ((bytes[0] & 0xFF) << 0 );
	}


	private static int extractModuleSize(int[][] pixelMatrix) {
    	int offset = 1, start = 0, ind = 0;
    	
    	while(start < pixelMatrix.length) {
    		if(isBlackPixel(pixelMatrix[start][start])) 
    			break;
    		start++;
    	}
    	ind = start + offset;
    	while(ind < pixelMatrix.length) {
    		while(!isBlackPixel(pixelMatrix[ind][ind])) 
    			ind--;
    		if(!isBlackPixel(pixelMatrix[ind+1][ind+1])) 
    			return ind+1-start;
    		else {
    			offset*=2;
    		}
    		ind = start + offset;
    	}
    	
		return ind;
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

	private static byte[] decodeConfiguredImage(QrImage qrImage, int bitLength, Position pos) {
		
		int byteLength = (int) Math.ceil((bitLength+0.0)/QrImage.BITS_IN_BYTE);
		byte[] decodedData = new byte[byteLength];
		
		for(int bitIndex = 0; bitIndex<bitLength; bitIndex++) {
			decodeModule(decodedData, qrImage, pos, bitIndex/QrImage.BITS_IN_BYTE, bitIndex%QrImage.BITS_IN_BYTE);
			pos.colModule++;
			QrImage.checkForColumnEnd(pos);
			
		}
		return decodedData;	
	}

	private static void decodeModule(byte[] decodedData, QrImage qrImage, Position pos, int dataIndex, int byteModulus) {
		
		boolean toggleBit, isBlackPixel;
		int rowPixel = pos.rowModule * qrImage.moduleSize;
		int colPixel = pos.colModule * qrImage.moduleSize;
		
		toggleBit = pos.colModule%3 == 0;	
		isBlackPixel = isBlackPixel(qrImage.pixelMatrix[rowPixel][colPixel]);
		
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
=======
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
		pos.rowModule = QrImage.MODULES_IN_MARGIN;
		pos.colModule = QrImage.MODULES_IN_MARGIN + QrImage.MODULES_IN_POS_DET_DIM;
		//extract image configuration and return cropped rotated image
		QrImage qrImage = configureImage(encodedImage, pos);
		//decode image data to byte array
		byte[] decodedData = decodeConfiguredImage(qrImage, qrImage.dataLength, pos);

		return new String(decodedData);
	}

	private static QrImage configureImage(BufferedImage encodedImage, Position pos) {
		QrImage qrImage = new QrImage();
		qrImage.pixelMatrix = convertTo2DUsingGetRGB(encodedImage);
		qrImage.moduleSize = extractModuleSize(qrImage.pixelMatrix);
		assert( qrImage.moduleSize != 0);
		//configureRotation(qrImage);	
		//configureCrop(qrImage);
		byte[] dataLengthBytes = decodeConfiguredImage(qrImage, QrImage.DATA_LEN_ENCODING_LENGTH, pos);
		qrImage.dataLength = byteArrayToInt(dataLengthBytes);
		
		return qrImage;
	}
	
	 // packing an array of bytes to an int - Little Endian
	static int byteArrayToInt(byte[] bytes) {
		int retVal = 0;
		for(int i = 0; i<bytes.length; i++) 
		     retVal |= ( (bytes[i] & 0xFF) << (i*QrImage.BITS_IN_BYTE) );
		
		return retVal;
	}


	private static int extractModuleSize(int[][] pixelMatrix) {
    	int offset = 1, start = 0, ind = 0;
    	
    	while(start < pixelMatrix.length) {
    		if(isBlackPixel(pixelMatrix[start][start])) 
    			break;
    		start++;
    	}
    	ind = start + offset;
    	while(ind < pixelMatrix.length) {
    		while(!isBlackPixel(pixelMatrix[ind][ind])) 
    			ind--;
    		if(!isBlackPixel(pixelMatrix[ind+1][ind+1])) 
    			return ind+1-start;
    		else {
    			offset*=2;
    		}
    		ind = start + offset;
    	}
    	
		return ind;
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

	private static byte[] decodeConfiguredImage(QrImage qrImage, int bitLength, Position pos) {
		
		int byteLength = (int) Math.ceil((bitLength+0.0)/QrImage.BITS_IN_BYTE);
		byte[] decodedData = new byte[byteLength];
		
		for(int bitIndex = 0; bitIndex<bitLength; bitIndex++) {
			decodeModule(decodedData, qrImage, pos, bitIndex/QrImage.BITS_IN_BYTE, bitIndex%QrImage.BITS_IN_BYTE);
			pos.colModule++;
			QrImage.checkForColumnEnd(pos);
			
		}
		return decodedData;	
	}

	private static void decodeModule(byte[] decodedData, QrImage qrImage, Position pos, int dataIndex, int byteModulus) {
		
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
>>>>>>> ef19dc0... adding files before rebase:EE Final Project/src/DisplayDecoder.java

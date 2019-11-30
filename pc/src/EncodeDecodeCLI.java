import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;
import javax.imageio.ImageIO;


public class EncodeDecodeCLI {
	
	public static void main(String... args) throws Exception {
		
		boolean continueProgram = true;
	    Scanner scanner = new Scanner(System.in);  // Create a Scanner object
	    String fileContent;
	    File inputFile;
	    
	    System.out.println("Enter Decode/Encode command\n");
	    
	    while(continueProgram) {
	    	String userCommand = scanner.nextLine().toLowerCase();  // Read user input
	    	if(!userCommand.startsWith("encode ") || !userCommand.startsWith("decode ")) {
	    		System.out.println("Usage: 'Encode [filepath] [target encoded filepath]'\n"
	    				+ "Usage: 'Decode [encoded filepath]");
	    		continue;
	    	}
	    	else {
	    		String[] splitedCommand = userCommand.split("\\s+");
	    		if(splitedCommand.length > 3 || (splitedCommand.length == 2 && splitedCommand[0].equals("encode"))
	    				|| (splitedCommand.length == 3 && splitedCommand[0].equals("decode")) ) {
		    		System.out.println("Usage: 'Encode [filepath] [target encoded filepath]'\n"
		    				+ "Usage: 'Decode [encoded filepath]");
	    			continue;
	    		}
	    		else {
	    			try {
	    				inputFile = new File(splitedCommand[1]);	
	    			}
	    			catch(Exception NullPointerException){
	    				System.out.println("Entered filepath doesn't exist.\n");
	    				continue;
	    			}
	    			
	    			switch (splitedCommand[0]) {
	    			case "encode":
	    				fileContent = new String(Files.readAllBytes(inputFile.toPath()));
	    				BufferedImage encodedImage = DisplayEncoder.encodeBytes(fileContent);
	    				File newPathQr = new File(splitedCommand[2]);
	    				ImageIO.write(encodedImage, "png", newPathQr);	    			
	    				break;
	    			case "decode":
	    				BufferedImage bufferedImageQr = ImageIO.read(inputFile);
	    				String decodedString = DisplayDecoder.decodeImage(bufferedImageQr);
	    				System.out.println("Decoded string is: "+decodedString+"\n");
	    				break;
	    			case "exit":
	    				continueProgram = false;
	    				System.out.println("Exiting..\n");
	    				break;
	    			default:
	    		    		System.out.println("Usage: 'Encode [filepath] [target encoded filepath]'\n"
	    		    				+ "Usage: 'Decode [encoded filepath]");
	    			}
	    				
	    		}
	    	}
	    }	    
	    scanner.close();
	}

}

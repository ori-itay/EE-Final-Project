package com.pc.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Parameters {
	private static final String ENCRYPTION_ALGORITHM = "encryptionAlgorithm";
	private static final String ENCRYPTION_MODE = "encryptionMode";
	private static final String IV_LENGTH = "ivLength";
	private static final String ENCODING_COLOR_LEVELS = "encodingColorLevels";
	private static final String PIXELS_IN_MODULE = "pixelsInModule";
	private static final String MODULES_IN_MARGIN = "modulesInMargin";
	
	private static final String PATH_TO_CONFIG_FILE = "resources/config.properties";
	
	
	private Parameters() {}
	
	public static int ivLength;
	public static String encryptionAlgorithm;
	public static String encryptionMode;
	public static int encodingColorLevels;
	public static int pixelsInModule;
	public static int modulesInMargin;
	
	static {
		InputStream inputStream = null;
		Properties prop;
		
		try {
			System.out.println(System.getProperty("user.dir"));
			inputStream = new FileInputStream(PATH_TO_CONFIG_FILE);
			prop = new Properties();
			prop.load(inputStream);
			
			encryptionAlgorithm = prop.getProperty(ENCRYPTION_ALGORITHM);
			ivLength = Integer.parseInt(prop.getProperty(IV_LENGTH));
			encryptionMode = prop.getProperty(ENCRYPTION_MODE);
			encodingColorLevels = Integer.parseInt(prop.getProperty(ENCODING_COLOR_LEVELS));
			pixelsInModule = Integer.parseInt(prop.getProperty(PIXELS_IN_MODULE));	
			modulesInMargin = Integer.parseInt(prop.getProperty(MODULES_IN_MARGIN));	
			
			inputStream.close();
			
		} catch (IOException e) {
			System.out.print(e.getMessage());
			ivLength = 0;
			encryptionAlgorithm = null;
			encryptionMode = null;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}

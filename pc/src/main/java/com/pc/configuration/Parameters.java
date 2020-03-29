package com.pc.configuration;

import com.pc.cli.EncodeDecodeCLI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;


public class Parameters {
//	private static final String ENCRYPTION_ALGORITHM = "encryptionAlgorithm";
//	private static final String ENCRYPTION_MODE = "encryptionMode";
//	private static final String IV_LENGTH = "ivLength";
//	private static final String ENCODING_COLOR_LEVELS = "encodingColorLevels";
//	private static final String PIXELS_IN_MODULE = "pixelsInModule";
//	private static final String MODULES_IN_MARGIN = "modulesInMargin";

	//private static final String CONFIG_FILE_NAME = "/resources/config.properties";


	private Parameters() {}

	public static int ivLength = 12;
	public static String encryptionAlgorithm = "AES";
	public static String encryptionMode = "AES/CBC/NoPadding";
	public static int encodingColorLevels = 2;
	public static int pixelsInModule = 10;
	public static int modulesInMargin = 2;

//	static {
//		InputStream inputStream = null;
//		Properties prop;
//
//		try {
//			File jarPath = new File(Parameters.class.getProtectionDomain().getCodeSource().getLocation().getPath());
//			String propertiesPath = jarPath.getParentFile().getParentFile().getAbsolutePath();
//			inputStream = new FileInputStream(propertiesPath + CONFIG_FILE_NAME);
//			prop = new Properties();
//			prop.load(inputStream);
//
//			encryptionAlgorithm = prop.getProperty(ENCRYPTION_ALGORITHM);
//			ivLength = Integer.parseInt(prop.getProperty(IV_LENGTH));
//			encryptionMode = prop.getProperty(ENCRYPTION_MODE);
//			encodingColorLevels = Integer.parseInt(prop.getProperty(ENCODING_COLOR_LEVELS));
//			pixelsInModule = Integer.parseInt(prop.getProperty(PIXELS_IN_MODULE));
//			modulesInMargin = Integer.parseInt(prop.getProperty(MODULES_IN_MARGIN));
//
//			inputStream.close();
//
//		} catch (IOException e) {
//			System.out.print(e.getMessage());
//			ivLength = 0;
//			encryptionAlgorithm = null;
//			encryptionMode = null;
//		}
//	}
























}

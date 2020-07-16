package com.pc;


import com.pc.checksum.Checksum;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import static javax.swing.JOptionPane.showMessageDialog;

public class Flow {
	
	private static final String encodedFilePath = "encodedImage.png";
	
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		//load image
		//sendUserSecretKey();
		playWithUi();



		try {
			initKeyStore();
			createDB();
			initServerThread();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		File inputFile = new File("200_200.jpg");		
		
		BufferedImage image = ImageIO.read(inputFile);
		byte[] imageBytes = FlowUtils.convertToBytesUsingGetRGB(image);
		IvParameterSpec iv = Encryptor.generateIv(Parameters.ivLength); 
		System.out.println("IV:" + Arrays.toString(iv.getIV()));
		byte[] chksumIV = Checksum.computeChecksum(iv.getIV());
		byte[] dimsArr = FlowUtils.getDimensionsArray(image);
		//SecretKey skey; 
		BufferedImage encodedImage;
		try {
			//skey = Encryptor.generateSymmetricKey();
			/* constant key */
			byte[] const_key = new byte[] {100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115};
			SecretKeySpec skeySpec = new SecretKeySpec(const_key, Parameters.encryptionAlgorithm);
			/****************/
			//SecretKeySpec skeySpec = new SecretKeySpec(skey.getEncoded(), Constants.ENCRYPTION_ALGORITHM);
			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);
			
			byte[] encryptedImg = Encryptor.encryptImage(imageBytes, generatedXorBytes);
			byte[] shuffledEncryptedImg = Shuffle.shuffleImgPixels(encryptedImg, iv);
			

			encodedImage = DisplayEncoder.encodeBytes(shuffledEncryptedImg, dimsArr, iv.getIV(),  chksumIV);
			ImageIO.write(encodedImage, "png", new File(encodedFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void playWithUi() {
		JFrame frame = new JFrame("VisualCrypto");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(300,300);



		JPanel panel = new JPanel(); // the panel is not visible in output
		JLabel label = new JLabel("Username:");
		JTextField tf = new JTextField(10); // accepts up to 10 characters
		JButton send = new JButton("Apply");
		send.addActionListener((actionEvent)-> {
			String email = tf.getText();
			if (email.isEmpty()) {
				showMessageDialog(null, "Error: username field is empty");
				return;
			}
			//TODO: cont
		});
		panel.add(label); // Components Added using Flow Layout
		panel.add(tf);
		panel.add(send);


		frame.getContentPane().add(BorderLayout.SOUTH, panel);
		frame.setVisible(true);
		int a=3;
		try {
			Robot robot = new Robot();
			//robot.
			Graphics2D g2 =null;
			//TODO: trying to make a rectangle shape, afterwards user press a button and we fetch the coordinates (50x50 rectangle), and use frames from that location for visualcrypto


		} catch (AWTException e) {
			e.printStackTrace();
		}

	}

	private static void initServerThread() {
		Thread t = new Server();
		t.start();
	}

	public static final String password = "barakitkin123";
	static Connection conn = null;
	static KeyStore ks = null;
	private static boolean openConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost?useSSL=false&serverTimezone=Asia/Jerusalem","admin",password);
			return true;
		} catch (Exception e) {
			System.out.println(Arrays.toString(e.getStackTrace()));
			return false;
		}
	}

	private static boolean closeConnection() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static final String keystoreName = "keyStore.jks";
	private static final String privateKeyAlias = "private";
	public static final SecretKey privateSecretKey = initKeyStore();


	public static SecretKey initKeyStore() {
		File ksFile = new File(keystoreName);
		char[] pwdArr = password.toCharArray();

		try {
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			SecretKey key = null;
			if (!ksFile.exists()) {
				ks.load(null, pwdArr);
				key = Encryptor.generateSymmetricKey();
				KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(key);
				KeyStore.ProtectionParameter pw = new KeyStore.PasswordProtection(pwdArr);
				ks.setEntry(privateKeyAlias, skEntry, pw);
				try (FileOutputStream fos = new FileOutputStream(keystoreName)) {
					ks.store(fos, pwdArr);
				} catch (IOException | CertificateException e) {
					e.printStackTrace();
				}
				return key;
			} else {
				try (FileInputStream fis = new FileInputStream(keystoreName)) {
					ks.load(fis, pwdArr);
					key = (SecretKey) ks.getKey(privateKeyAlias, pwdArr);
					return key;
				}

			}
		} catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
			e.printStackTrace();
		}
		return null;
	}




	public static boolean createDB() throws SQLException, ClassNotFoundException {
		if (!openConnection())
			return false;
		try {
			Statement createDB = conn.createStatement();
			createDB.execute("CREATE DATABASE IF NOT EXISTS visual_crypto");
			conn.setCatalog("visual_crypto");
			Statement createTable = conn.createStatement();
			createTable.execute("CREATE TABLE IF NOT EXISTS Users (email VARCHAR(40), pw VARCHAR(255))");
			//createTable.execute(); USE Users
		}
		catch (SQLException e) {
			e.printStackTrace();
			closeConnection();
			return false;
		}
		//closeConnection();
		return true;
	}



}

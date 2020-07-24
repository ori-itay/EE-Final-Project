package com.pc;


import com.pc.checksum.Checksum;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static javax.swing.JOptionPane.showMessageDialog;

public class Flow {

	private static final JFrame frame = new JFrame("VisualCrypto");
	private static String username;
	private static SecretKey userSecretKey;
	private static JLabel imgLabel;

	private static ScheduledExecutorService executor;
	private static Robot robot = null;
	protected static Rectangle screenRect;

	static {
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(ScreenCaptureRectangle::assignCapturedRectangle);
		startUI();

		if (!createDB()) {
			System.out.println("No connection to DB");
			return;
		}
		initServerThread();
	}

	public static void flow(BufferedImage image) {
		byte[] imageBytes = FlowUtils.convertToBytesUsingGetRGB(image);
		//IvParameterSpec iv = Encryptor.generateIv(Parameters.ivLength);
		IvParameterSpec iv = new IvParameterSpec(new byte[]{1,2,3,4,5,6,7,8,9,10,11,12});

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
			//SecretKeySpec skeySpec = new SecretKeySpec(userSecretKey.getEncoded(), Parameters.encryptionAlgorithm);
			byte[] generatedXorBytes = EncryptorDecryptor.generateXorBytes(skeySpec, iv);

			byte[] encryptedImg = Encryptor.encryptImage(imageBytes, generatedXorBytes);
			byte[] shuffledEncryptedImg = Shuffle.shuffleImgPixels(encryptedImg, iv);

			encodedImage = DisplayEncoder.encodeBytes(shuffledEncryptedImg, dimsArr, iv.getIV(),  chksumIV);

			imgLabel.setIcon(new ImageIcon(new ImageIcon(encodedImage).getImage().getScaledInstance(-1 ,frame.getContentPane().getBounds().height,Image.SCALE_SMOOTH)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	private static void getRectangleFromUser() {
		final Rectangle[] rect = new Rectangle[1];
		//SwingUtilities.invokeAndWait(()-> rect[0] = ScreenCaptureRectangle.getCapturedRectangle());


	}


	private static void startUI() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		Dimension dim = new Dimension(gd.getDisplayMode().getWidth(),gd.getDisplayMode().getHeight() - 25);
		//frame.setPreferredSize(dim);

		JLabel loggedInAs = new JLabel("Welcome: ");
		JPanel panel = new JPanel();
		JLabel usernameLabel = new JLabel("Username:");
		JTextField tf = new JTextField(10);
		JButton apply = new JButton("Apply");
		apply.addActionListener((actionEvent)-> {
			String email = tf.getText();
			if (email.isEmpty()) {
				showMessageDialog(null, "Error: username field is empty");
				return;
			}
			username = email;
			userSecretKey = fetchUserKey(username);
			if (userSecretKey == null) {
				System.out.println("fetchUserKey returned null!");
				return;
			}
			loggedInAs.setText("Logged in as: " + email);
			tf.setText("");
		});

		imgLabel = new JLabel("", SwingConstants.CENTER);

		JToggleButton toggleButton = new JToggleButton("Off");
		toggleButton.addItemListener((itemEvent) -> {
			int state = itemEvent.getStateChange();
			if (state == ItemEvent.SELECTED) {
				executor = Executors.newSingleThreadScheduledExecutor();
				executor.scheduleAtFixedRate(()-> {
					BufferedImage img = robot.createScreenCapture(screenRect);
					flow(img);
				}, 0, 100, TimeUnit.MILLISECONDS);
				toggleButton.setText("On");
			} else {
				executor.shutdown();
				toggleButton.setText("Off");
			}
		});

		panel.add(usernameLabel);
		panel.add(tf);
		panel.add(apply);


		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.add(loggedInAs, BorderLayout.WEST);
		leftPanel.add(imgLabel, BorderLayout.CENTER);

		frame.add(leftPanel, BorderLayout.CENTER);

		JPanel rightPanel = new JPanel();
		rightPanel.add(panel);

		JPanel rightCenterPanel = new JPanel(new BorderLayout());
		toggleButton.setPreferredSize(new Dimension(52, gd.getDisplayMode().getHeight()));
		rightCenterPanel.add(toggleButton, BorderLayout.CENTER);
		rightPanel.add(rightCenterPanel);
		frame.add(rightPanel, BorderLayout.EAST);

		frame.pack();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		frame.setVisible(true);
	}

	private static SecretKey fetchUserKey(String username) {
		String insetStr = "SELECT * FROM Users WHERE email = ?";

		PreparedStatement getUserKey;
		try {
			getUserKey = Flow.conn.prepareStatement(insetStr);
			getUserKey.setString(1, username);
			ResultSet rs = getUserKey.executeQuery();
			if (rs.next()) {
				String pw = rs.getString("pw");
				if (pw != null) {
					byte[] bytesKeyEncrypted = Base64.getDecoder().decode(pw);
					final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
					cipher.init(Cipher.DECRYPT_MODE, privateSecretKey, new IvParameterSpec(new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));
					byte[] decrypted = cipher.doFinal(bytesKeyEncrypted);
					return new SecretKeySpec(decrypted, 0, decrypted.length, "AES");
				}
			}
		} catch (SQLException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
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
			conn = DriverManager.getConnection("jdbc:mysql://localhost?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem","admin",password);
			return true;
		} catch (Exception e) {
			System.out.println(Arrays.toString(e.getStackTrace()));
			return false;
		}
	}

	private static void closeConnection() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static final String keystoreName = "keyStore.jks";
	private static final String privateKeyAlias = "private";
	public static final SecretKey privateSecretKey = initKeyStore();


	public static SecretKey initKeyStore() {
		File ksFile = new File(keystoreName);
		char[] pwdArr = password.toCharArray();

		try {
			ks = KeyStore.getInstance("pkcs12");
			SecretKey key;
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




	public static boolean createDB() {
		if (!openConnection())
			return false;
		try {
			Statement createDB = conn.createStatement();
			createDB.execute("CREATE DATABASE IF NOT EXISTS visual_crypto");
			conn.setCatalog("visual_crypto");
			Statement createTable = conn.createStatement();
			createTable.execute("CREATE TABLE IF NOT EXISTS Users (email VARCHAR(40), pw VARCHAR(255))");
		}
		catch (SQLException e) {
			e.printStackTrace();
			closeConnection();
			return false;
		}
		return true;
	}



}

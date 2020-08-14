package com.pc;


import com.pc.checksum.Checksum;
import com.pc.configuration.Constants;
import com.pc.configuration.Parameters;
import com.pc.encoderDecoder.DisplayEncoder;
import com.pc.encryptorDecryptor.EncryptorDecryptor;
import com.pc.encryptorDecryptor.encryptor.Encryptor;
import com.pc.shuffleDeshuffle.shuffle.Shuffle;
import org.imgscalr.Scalr;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * This class is responsible for the PC GUI flow
 */
public class Flow {

	private static final JFrame frame = new JFrame("VisualCrypto");
	private static final JLabel gammaIsText = new JLabel("gamma: 0.75");

	private static int secondsPerFrame = 700;
	private static final double SCALE_FACTOR = 3;
	private static String username;
	private static SecretKey userSecretKey;
	private static JLabel imgLabel;

	private static ScheduledExecutorService executor;
	protected static Rectangle screenRect;
	private static JLabel loggedInAs = new JLabel();

	private static final String LAST_LOGIN = "lastlogin";
	public static double gamma = 0.75;
	public static final String password = "barakitkin123";
	static Connection conn = null;

	private static final String keystoreName = "keyStore.jks";
	private static final String privateKeyAlias = "private";
	public static final SecretKey privateSecretKey = initKeyStore();


	/**
	 *  Init the database, server and the GUI
	 */
	public static void main(String[] args) {
		if (!initDB()) {
			System.out.println("No connection to DB");
			return;
		}
		initServerThread();

		SwingUtilities.invokeLater(ScreenCaptureRectangle::assignCapturedRectangle);
		startUI();
	}

	/**
	 * VisualCrypto flow for creating an encoded image
	 * @param image - The original image
	 */
	public static void flow(BufferedImage image) {
		byte[] imageBytes = FlowUtils.convertToBytesUsingGetRGB(image);

		IvParameterSpec iv = Encryptor.generateIv(Parameters.ivLength);
		//IvParameterSpec iv = new IvParameterSpec(new byte[]{1,2,3,4,5,6,7,8,9,10,11,12});

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

	/**
	 * Boots up the UI
	 */
	private static void startUI() {
		String emailStr;

		if ((emailStr = fetchLastLoggedInUser()) != null) {
			loggedInAs.setText("Welcome: " + emailStr);
			username = emailStr;
			userSecretKey = fetchUserKey(username);
			if (userSecretKey == null) {
			    JOptionPane.showMessageDialog(null ,"Warning: could not find user '" + username +"'");
			}
		} else {
			registerUI();
		}

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

		JPanel panel = new JPanel();
		imgLabel = new JLabel("", SwingConstants.CENTER);

		JToggleButton toggleButton = new JToggleButton("Off");
		toggleButton.addItemListener((itemEvent) -> {
			int state = itemEvent.getStateChange();
			if (state == ItemEvent.SELECTED) {
				repeatedEncodeTask();
				toggleButton.setText("On");
			} else {
				executor.shutdown();
				toggleButton.setText("Off");
			}
		});

		JLabel logOut = new JLabel(" (Log out)");
		logOut.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		logOut.setForeground(Color.BLUE);
		logOut.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				removeLastLoggedFromDB();
				registerUI();
			}
		});

		JButton changeRectangleBtn = new JButton("Switch area");
		changeRectangleBtn.setPreferredSize(new Dimension(10,10));
		changeRectangleBtn.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Rectangle screenSizes = new Rectangle(0, 0, 0, 0);
				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				GraphicsDevice[] gs = ge.getScreenDevices();

				for (GraphicsDevice g : gs) {
					screenSizes = screenSizes.union(g.getDefaultConfiguration().getBounds());
				}
				screenRect = new Rectangle(screenSizes.x, screenSizes.y + ScreenCaptureRectangle.jFrame.getInsets().top, screenSizes.width, screenSizes.height);
				BufferedImage screen = ScreenCaptureRectangle.robot.createScreenCapture(screenRect);
				final BufferedImage screenCopy = new BufferedImage(
						screen.getWidth(),
						screen.getHeight(),
						screen.getType());
				ScreenCaptureRectangle.screen = screen;
				ScreenCaptureRectangle.screenCopy = screenCopy;
				ScreenCaptureRectangle.screenLabel.setIcon(new ImageIcon(screenCopy));
				ScreenCaptureRectangle.repaint(screen, screenCopy);
				ScreenCaptureRectangle.screenLabel.repaint();

				ScreenCaptureRectangle.jFrame.setVisible(true);
			}
		});

		JLabel encodingLevelsText = new JLabel("encodingLevels: " + Parameters.encodingColorLevels);
		JLabel encodingLevelsChange = new JLabel("  (Change)");
		encodingLevelsChange.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		encodingLevelsChange.setForeground(Color.GRAY);
		encodingLevelsChange.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				String encodingLevelsInput = JOptionPane.showInputDialog("Please enter encoding color levels (2,4, or 8)");
				try {
					int encodingLevels = Integer.parseInt(encodingLevelsInput);
					if (encodingLevels != 2 && encodingLevels != 4 && encodingLevels != 8) {
						throw new NumberFormatException("Invalid value");
					} else {
						Parameters.encodingColorLevels = encodingLevels;
						Constants.COLOR_SCALE_DELTA = Math.floorDiv(255 , (Parameters.encodingColorLevels-1));
						Constants.ENCODING_BIT_GROUP_SIZE = (int) (Math.log(Parameters.encodingColorLevels)/ Math.log(2) );
						Constants.BIT_GROUP_MASK_OF_ONES = (1 << Constants.ENCODING_BIT_GROUP_SIZE) -1;
						encodingLevelsText.setText("encodingLevels: " + encodingLevels);
					}

				} catch (NumberFormatException numberFormatException) {
					JOptionPane.showMessageDialog(null, "Invalid input!");
				}
			}
		});

		JLabel gammaChange = new JLabel("  (Change)");
		gammaChange.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		gammaChange.setForeground(Color.GRAY);
		gammaChange.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				String gammaInput = JOptionPane.showInputDialog("Please enter the gamma factor (from 0 to 1 inclusive)");
				try {
					double gamma = Double.parseDouble(gammaInput);
					if (gamma <= 0 || gamma > 1) {
						throw new NumberFormatException("Invalid gamma value!");
					} else {
						Flow.gamma = gamma;
						gammaIsText.setText("gamma: " + gamma);
					}
				} catch (NumberFormatException numberFormatException) {
					JOptionPane.showMessageDialog(null, "Invalid input!");
				}
			}
		});

		JLabel timeBetweenEncoding = new JLabel("fps: 1.42");
		JLabel changeTimeBetweenEncoding = new JLabel("  (Change)");
		changeTimeBetweenEncoding.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		changeTimeBetweenEncoding.setForeground(Color.GRAY);
		changeTimeBetweenEncoding.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				String gammaInput = JOptionPane.showInputDialog("Please enter frame per second value:");
				try {
					double fps = Double.parseDouble(gammaInput);
					if (fps <= 0) {
						throw new NumberFormatException("Invalid fps value!");
					} else {
						Flow.secondsPerFrame = (int) (1000/fps);
						executor.shutdown();
						repeatedEncodeTask(); // start the tasks with the updated time between frames
						timeBetweenEncoding.setText("fps: " + fps);
					}
				} catch (NumberFormatException numberFormatException) {
					JOptionPane.showMessageDialog(null, "Invalid input!");
				}
			}
		});


		JPanel leftPanel = new JPanel(new BorderLayout());
		JPanel innerLeftPanel = new JPanel(new GridLayout(5,2));
		innerLeftPanel.add(loggedInAs);
		innerLeftPanel.add(logOut);
		innerLeftPanel.add(gammaIsText);
		innerLeftPanel.add(gammaChange);
		innerLeftPanel.add(encodingLevelsText);
		innerLeftPanel.add(encodingLevelsChange);
		innerLeftPanel.add(timeBetweenEncoding);
		innerLeftPanel.add(changeTimeBetweenEncoding);
		innerLeftPanel.add(changeRectangleBtn);

		leftPanel.add(innerLeftPanel, BorderLayout.WEST);
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
		frame.setVisible(true);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		ScreenCaptureRectangle.jFrame.toFront();
	}

	/**
	 * This is the task of sampling the region of interest and do the VisualCyrpto logic on it, and display it to the user
	 */
	private static void repeatedEncodeTask() {
		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(()-> {
			BufferedImage img = ScreenCaptureRectangle.robot.createScreenCapture(screenRect);
			BufferedImage scaledImg = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, (int)(img.getWidth()/SCALE_FACTOR), (int)(img.getHeight()/SCALE_FACTOR));
			flow(scaledImg);
		}, 0, secondsPerFrame, TimeUnit.MILLISECONDS);
	}

	/**
	 * Removes the user who was last logged to the system
	 * @return whether it failed or succeeded
	 */
	private static boolean removeLastLoggedFromDB() {
		String removeLastLoggedInStr = "DELETE FROM Users WHERE pw='lastlogin'";

		try {
			PreparedStatement insert = Flow.conn.prepareStatement(removeLastLoggedInStr);
			insert.execute();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Shows the register UI
	 */
	private static void registerUI() {
		String emailStr;
		while (true) {
			emailStr = JOptionPane.showInputDialog("Enter username:");
			if (emailStr == null || emailStr.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Error: please enter a valid username");
			} else {
				loggedInAs.setText("Welcome: " + emailStr);
				insertLastLoggedInToDB(emailStr);
				username = emailStr;
				userSecretKey = fetchUserKey(username);
				if (userSecretKey == null) {
                    JOptionPane.showMessageDialog(null ,"Warning: could not find user '" + username +"'");
					return;
				}
				break;
			}
		}
	}

	/**
	 * Inset to the DB the most recent logged in user
	 * @param emailStr - The email address of that user
	 * @return whether the operation succeeded
	 */
	private static boolean insertLastLoggedInToDB(String emailStr) {
		String insetStr = "INSERT INTO Users (email,pw) VALUES(?, ?)";

		try {
			PreparedStatement insert = Flow.conn.prepareStatement(insetStr);
			insert.setString(1, emailStr);
			insert.setString(2, LAST_LOGIN);
			insert.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Get the most recent logged in user
	 * @return the username of that user
	 */
	private static String fetchLastLoggedInUser() {
		String lastLoggedInStr = "SELECT * FROM Users WHERE pw='" + LAST_LOGIN +"'";
		PreparedStatement getUsername;

		try {
			getUsername = Flow.conn.prepareStatement(lastLoggedInStr);
			ResultSet rs = getUsername.executeQuery();
			if (rs.next()) {
				return rs.getString("email");
			}
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return null;
	}

	/**
	 * Get the secret key that belongs to username
	 * @param username - The username
	 * @return the secret key of that user
	 */
	private static SecretKey fetchUserKey(String username) {
		String insertStr = "SELECT * FROM Users WHERE email = ?";

		PreparedStatement getUserKey;
		try {
			getUserKey = Flow.conn.prepareStatement(insertStr);
			getUserKey.setString(1, username);
			ResultSet rs = getUserKey.executeQuery();
			if (rs.next()) {
				String pw = rs.getString("pw");
				if (pw != null && !pw.equals("lastlogin")) {
					byte[] bytesKeyEncrypted = Base64.getDecoder().decode(pw);
					final Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
					cipher.init(Cipher.DECRYPT_MODE, privateSecretKey, new IvParameterSpec(new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));
					byte[] decrypted = cipher.doFinal(bytesKeyEncrypted);
					return new SecretKeySpec(decrypted, 0, decrypted.length, "AES");
				}
			} else {
				System.out.println("Error: no username: " + username + " exists!");
			}
		} catch (SQLException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Starts the server which listens for secret key exchange requests
	 */
	private static void initServerThread() {
		Thread t = new Server();
		t.start();
	}

	/**
	 * Open connection to the DB
	 * @return whether the operation succeeded
	 */
	private static boolean openConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:users.db");
			return true;
		} catch (Exception e) {
			System.out.println(Arrays.toString(e.getStackTrace()));
			return false;
		}
	}

	/**
	 * Close connection to the DB
	 */
	private static void closeConnection() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Init the key store
	 * @return the secret key that is used to encrypt entries in the DB
	 */
	public static SecretKey initKeyStore() {
		File ksFile = new File(keystoreName);
		char[] pwdArr = password.toCharArray();

		try {
			KeyStore ks = KeyStore.getInstance("pkcs12");
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

	/**
	 * Init the database
	 * @return whether the operation succeeded
	 */
	public static boolean initDB() {
		if (!openConnection())
			return false;
		try {
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

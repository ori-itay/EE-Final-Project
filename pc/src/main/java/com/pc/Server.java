package com.pc;

import com.pc.encryptorDecryptor.encryptor.Encryptor;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;

import static com.pc.Flow.privateSecretKey;

public class Server  extends Thread {

    private Socket socket   = null;
    private ServerSocket server   = null;
    private BufferedReader in       =  null;

    @Override
    public void run() {
        try {
            server = new ServerSocket(32326);
            server.setReuseAddress(true);
            System.out.println("Server started");

            System.out.println("Waiting for a client ...");

            while (true) {
                socket = server.accept();
                System.out.println("Client accepted");

                // takes input from the client socket
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String line = "";

                // reads message from client until "over" is sent
                while (!line.equals("over"))
                {
                    try
                    {
                        line = in.readLine();
                        if (line != null && line.startsWith("keyrequest")) {
                            String[] strArr = line.split(":");
                            if (strArr.length == 2) {
                                String userEmail = strArr[1];
                                if (!userEmail.contains("@")) {
                                    continue;
                                }

                                SecretKey skey = Encryptor.generateSymmetricKey();

                                insertEntry(userEmail, skey.getEncoded(), privateSecretKey);

                                String content = "Welcome!\nYour secret key is (base64):\n" + Base64.getEncoder().encodeToString(skey.getEncoded());
                                String subject = "VisualCrypto Secret Key";
                                sendMail(userEmail, subject, content);
                            }
                        }
                        System.out.println(line);
                    } catch (NoSuchAlgorithmException  | IOException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                socket.close();
                in.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (server != null) {
                    server.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void sendMail(String to, String subject, String content) {
        final String username = "tau.visualcrypto@gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, Flow.password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("tau.visualcrypto@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean insertEntry(String email, byte[] userSecretKey, SecretKey privateSecretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final Cipher cipher = Cipher.getInstance("AES/CTR/PKCS5Padding");
        try {
            cipher.init(Cipher.ENCRYPT_MODE, privateSecretKey, new IvParameterSpec(new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}));
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            return false;
        }
        byte[] encrypted = cipher.doFinal(userSecretKey);


        String insetStr = "INSERT INTO Users (email,pw) VALUES(?, ?)";

        try {
            PreparedStatement insert = Flow.conn.prepareStatement(insetStr);
            insert.setString(1, email);
            insert.setString(2, Base64.getEncoder().encodeToString(encrypted));
            insert.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

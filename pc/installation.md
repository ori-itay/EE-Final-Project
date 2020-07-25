(1) git clone https://github.com/ori-itay/EE-Final-Project.git

(2) download and install mysql
For windows 10:
https://dev.mysql.com/downloads/windows/installer/8.0.html
Make sure to create the following user:
```
username: admin
password: barakitkin123
```
(3) Copy the PC jar to the android app:
```shell script
cd EE-Final-Project/pc
./buildAndCopyJar.sh
```
(4) To run the PC program:

The following way can't establish connection to mysql DB on my computer (classpath issues related):
```shell script
java -jar all-pc.java
```
Instead, import the pc project 'EE-Final-Project/pc' in intellij and run Flow.java
Make sure there aren't any messages as "No connection to DB" etc.

(5) Build an apk and install it on your android device:
Make sure your device is connected to the computer via USB cable and run (instructions can also be found [here)](https://developer.android.com/studio/build/building-cmdline#DebugMode):
Note: you might need to install an android driver for your phone and enable debugging mode on it.
```shell script
cd ../mobile/VisualCrypto/
./gradlew installDebug
```
(6) 

    - Locate "VisualCrypto" app. Open it, grant it the permissions it asks for.
    - Enable wifi and join the same network the PC program is running in.
    - Check the local IP of the pc via ipconfig and put that IP in the "Enter server IP address" on your phone.
    - Also enter your email in the "Enter your email address" field.
    - Click REGISTER (at this point an output of "Client accepted" should appear in the PC program output. If there isn't, make sure both are connected to the same network and no firewall is blocking it. From our experience, even Windows Firewall blocked it for one of us).
(7) Once you get the email, copy the secret key and paste it and click OK.

(8) Return to the PC program.

    - Enter your username (=email address) that you registered with in the android app and click OK.
    - Choose a rectangle of interest with your mouse, which will be the area that will be encrypted and decoded. After choosing a rectangle, click OK or press Enter.
    - To your right, a long button with the text "Off". Once clicking on it (it's a toggle button), it will start encoding the rectangle area you provided, and display it to you (encrypted of course).
 
 (9) 
 
    - Make sure to capture the photos when your phone is in horizontal mode. In the android app, click "Video Mode".
    - Click "OFF" (It's a toggle button as well) to start capture frames. Click on it again if you want to stop.
    - The decoded frames should appear on your phone's screen.


Enjoy!
 


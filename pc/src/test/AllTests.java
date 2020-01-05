package test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

import test.encoderDecoder.EncoderDecoderTest;
import test.encryptorDecryptor.decryptor.DecryptorTest;
import test.encryptorDecryptor.encryptor.EncryptorTest;

@RunWith(JUnitPlatform.class)
@SelectClasses({EncryptorTest.class, DecryptorTest.class/*, EncoderDecoderTest.class*/})
public class AllTests {
}

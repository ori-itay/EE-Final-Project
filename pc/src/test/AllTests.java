package test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

import test.encoderDecoder.EncoderDecoderTest;
import test.encryptor.EncryptorTest;

@RunWith(JUnitPlatform.class)
@SelectClasses({EncryptorTest.class, EncoderDecoderTest.class})
public class AllTests {
}

package test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

import com.checksum.Checksum;

import test.encoderDecoder.EncoderDecoderTest;
import test.encryptorDecryptor.decryptor.DecryptorTests;
import test.encryptorDecryptor.encryptor.EncryptorTests;
import test.shuffleDeshuffle.deshuffle.DeshuffleTests;
import test.shuffleDeshuffle.shuffle.ShuffleTests;

@RunWith(JUnitPlatform.class)
@SelectClasses({EncryptorTests.class, Checksum.class, DecryptorTests.class, ShuffleTests.class, DeshuffleTests.class/*, EncoderDecoderTest.class*/})
public class AllTests {
}

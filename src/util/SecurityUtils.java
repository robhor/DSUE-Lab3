package util;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Hex;

public class SecurityUtils {
	private static final String CIPHER = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	
	private static final Logger logger = Logger.getLogger(SecurityUtils.class.getSimpleName());
	
	/**
	 * @param size length of the number in bytes
	 * @return a random number
	 */
	public static byte[] generateNumber(int size) {
		byte[] number = new byte[size];
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(number);
		
		return number;
	}
	
	/**
	 * Decode a RSA-encrypted message
	 * @param message
	 * @param key
	 * @return
	 */
	public static byte[] decryptRSA(byte[] message, Key key) {
		return transformRSA(message, key, Cipher.DECRYPT_MODE);
	}
	
	/**
	 * Encrypt a message using RSA
	 * @param message
	 * @param key
	 * @return
	 */
	public static byte[] encryptRSA(byte[] message, Key key) {
		return transformRSA(message, key, Cipher.ENCRYPT_MODE);
	}
	
	public static byte[] hmacSHA256(byte[] message, Key key) {
		Mac hMac;
		try {
			hMac = Mac.getInstance(HMAC_ALGORITHM);
			hMac.init(key);
			hMac.update(message);
			return hMac.doFinal();
		} catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "No such algorithm: " + e.getMessage());
		} catch (InvalidKeyException e) {
			logger.log(Level.SEVERE, "Invalid key: " + e.getMessage());
		}
		
		return null;
	}
	
	private static byte[] transformRSA(byte[] message, Key key, int mode) {
		try {
			Cipher crypt = Cipher.getInstance(CIPHER);
			crypt.init(mode, key);
			return crypt.doFinal(message);
		} catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "No such algorithm: " + e.getMessage());
		} catch (NoSuchPaddingException e) {
			logger.log(Level.SEVERE, "No such padding: " + e.getMessage());
		} catch (InvalidKeyException e) {
			logger.log(Level.SEVERE, "Invalid key: " + e.getMessage());
		} catch (IllegalBlockSizeException e) {
			logger.log(Level.SEVERE, "Illegal block size: " + e.getMessage());
		} catch (BadPaddingException e) {
			logger.log(Level.SEVERE, "Bad padding: " + e.getMessage());
		}
		
		return null;
	}
	
	
	public static PrivateKey getPrivateKey(String path, final String password) throws IOException {
		PEMReader pemreader = new PEMReader(new FileReader(path), new PasswordFinder() {
			public char[] getPassword() { return password.toCharArray(); }
		});
		
		PrivateKey privateKey;
		try {
			KeyPair keyPair = (KeyPair) pemreader.readObject();
			 privateKey = keyPair.getPrivate();
		} finally {
			pemreader.close();
		}
		
		return privateKey;
	}
	
	
	public static PublicKey getPublicKey(String path) throws IOException {
		PEMReader pemreader = new PEMReader(new FileReader(path));
		PublicKey pk = (PublicKey) pemreader.readObject();
		pemreader.close();
		
		return pk;
	}
	
	
	public static SecretKey getSecretKey(int keysize) {
		try {
			
			KeyGenerator generator = KeyGenerator.getInstance("AES");
			generator.init(keysize);
			return generator.generateKey();
			
		} catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, "No such algorithm: " + e.getMessage());
			return null;
		}
	}
	
	
	public static Key getClientKey(String path) throws IOException {
		byte[] keyBytes = new byte[1024];
		FileInputStream fis = new FileInputStream(path);
		fis.read(keyBytes);
		fis.close();
		byte[] input = Hex.decode(keyBytes);
		return new SecretKeySpec(input, HMAC_ALGORITHM);
	}
}

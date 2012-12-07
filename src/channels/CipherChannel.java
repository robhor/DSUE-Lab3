package channels;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * Applies a cipher when writing and reading
 */
public class CipherChannel implements Channel {
	private Logger logger = Logger.getLogger(CipherChannel.class.getSimpleName());
	
	private Channel channel;
	private Cipher encryptCipher, decryptCipher;
	
	public CipherChannel(Channel channel) {
		this.channel = channel;
	}
	
	public void setCipher(Cipher encryptionCipher, Cipher decryptionCipher) {
		this.encryptCipher = encryptionCipher;
		this.decryptCipher = decryptionCipher;
	}

	@Override
	public byte[] read() throws IOException {
		if (decryptCipher == null) return channel.read();
		
		try {
			byte[] msg = channel.read();
			if (msg == null) return null;
			
			return decryptCipher.doFinal(msg);
		} catch (IllegalBlockSizeException e) {
			throw new IOException("Could not decrypt message: " + e.getMessage());
		} catch (BadPaddingException e) {
			throw new IOException("Could not decrypt message: " + e.getMessage());
		}
	}

	@Override
	public void send(byte[] message) {
		if (encryptCipher == null) {
			channel.send(message);
			return;
		}
		
		try {
			channel.send(encryptCipher.doFinal(message));
		} catch (IllegalBlockSizeException e) {
			logger.log(Level.SEVERE, "Could not encrypt message: " + e.getMessage());
		} catch (BadPaddingException e) {
			logger.log(Level.SEVERE, "Could not encrypt message: " + e.getMessage());
		}
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}
}

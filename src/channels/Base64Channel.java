package channels;

import java.io.IOException;

import org.bouncycastle.util.encoders.Base64;

public class Base64Channel implements Channel {
	private Channel channel;
	
	public Base64Channel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public byte[] read() throws IOException {
		byte[] msg = channel.read();
		if (msg == null) return null;
		
		return Base64.decode(msg);
	}

	@Override
	public void send(byte[] message) {
		byte[] encoded = Base64.encode(message);
		channel.send(encoded);
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

}

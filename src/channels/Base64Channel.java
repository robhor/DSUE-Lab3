package channels;

import java.io.IOException;

import org.bouncycastle.util.encoders.Base64;

public class Base64Channel implements Channel {
	private Channel channel;
	
	public Base64Channel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public String read() throws IOException {
		String msg = channel.read();
		return new String(Base64.decode(msg));
	}

	@Override
	public void send(String message) throws IOException {
		byte[] encoded = Base64.encode(message.getBytes());
		channel.send(new String(encoded));
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

}

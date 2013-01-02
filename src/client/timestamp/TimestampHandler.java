package client.timestamp;

import java.io.IOException;
import java.net.Socket;

import org.bouncycastle.util.encoders.Base64;

import util.SecurityUtils;
import channels.Base64Channel;
import channels.Channel;
import channels.TCPChannel;
import client.TCPProtocol;

public class TimestampHandler implements Runnable {
	private TimestampServer server;
	private Channel channel;
	
	public TimestampHandler(TimestampServer server, Socket client) {
		try {
			this.server = server;
			this.channel = new TCPChannel(client);
			this.channel = new Base64Channel(channel);
		} catch (IOException e) {
			System.err.println("Opening channel failed");
		}
	}
	
	@Override
	public void run() {
		if (channel == null) return;
		
		try {
			byte[] msg;
			while((msg = channel.read()) != null) {
				handleMessage(new String(msg));
			}
		} catch (IOException e) {
			// closed
		}
		close();
	}
	
	private void handleMessage(String msg) {
		String[] token = msg.split(" ");
		if (token.length != 3 || server.getSigningKey() == null) {
			channel.send(TCPProtocol.RESPONSE_FAIL.getBytes());
			return;
		}
		
		long time = System.currentTimeMillis();
		
		String answer = String.format("%s %s %s %d", TCPProtocol.RESPONSE_TIMESTAMP, token[1], token[2], time);
		System.out.println("Signing: " + answer);
		answer += " " + sign(answer);
		
		channel.send(answer.getBytes());
	}
	
	private String sign(String msg) {
		byte[] signed = SecurityUtils.sign(msg.getBytes(), server.getSigningKey());
		return new String(Base64.encode(signed));
	}
	
	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			return;
		}
	}
}
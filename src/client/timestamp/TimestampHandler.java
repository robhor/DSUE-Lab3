package client.timestamp;

import java.io.IOException;
import java.net.Socket;

import channels.Channel;
import channels.TCPChannel;
import client.TCPProtocol;

public class TimestampHandler implements Runnable {
	private Channel channel;
	
	public TimestampHandler(Socket client) {
		try {
			this.channel = new TCPChannel(client);
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
		if (token.length != 3) {
			channel.send(TCPProtocol.RESPONSE_FAIL.getBytes());
			return;
		}
		
		long time = System.currentTimeMillis();
		
		String answer = String.format("%s %s %s %d", TCPProtocol.RESPONSE_TIMESTAMP, token[1], token[2], time);
		
		// TODO sign
		
		channel.send(answer.getBytes());
	}
	
	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			return;
		}
	}
}
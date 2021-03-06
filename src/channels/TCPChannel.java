package channels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPChannel implements Channel {
	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;
	
	public TCPChannel(Socket socket) throws IOException {
		this.socket = socket;
		
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new PrintWriter(socket.getOutputStream(), true);
	}

	@Override
	public byte[] read() throws IOException {
		String line = reader.readLine();
		return (line == null) ? null : line.getBytes();
	}

	@Override
	public void send(byte[] message) {
		writer.println(new String(message));
	}

	@Override
	public void close() throws IOException {
		socket.close();
		reader.close();
		writer.close();
	}
}

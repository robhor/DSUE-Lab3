package server.bean;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Encapsulates all information about a connected client 
 */
public class Client {
	private Socket tcpSocket;
	private int    udpPort;
	
	private BufferedReader reader;
	private PrintWriter    writer;
	
	public Socket getTcpSocket() {
		return tcpSocket;
	}
	public void setTcpSocket(Socket tcpSocket) {
		this.tcpSocket = tcpSocket;
	}
	public int getUdpPort() {
		return udpPort;
	}
	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}
	public BufferedReader getReader() {
		return reader;
	}
	public void setReader(BufferedReader reader) {
		this.reader = reader;
	}
	public PrintWriter getWriter() {
		return writer;
	}
	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}
}

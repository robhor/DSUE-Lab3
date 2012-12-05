package server.bean;

import java.net.InetAddress;

import channels.Channel;

/**
 * Encapsulates all information about a connected client 
 */
public class Client {
	private Channel channel;
	private InetAddress inetAddress;
	private int     udpPort;
		
	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	public int getUdpPort() {
		return udpPort;
	}
	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}
	public InetAddress getInetAddress() {
		return inetAddress;
	}
	public void setInetAddress(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}
}

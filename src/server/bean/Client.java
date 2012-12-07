package server.bean;

import java.net.InetAddress;

import channels.Channel;
import channels.CipherChannel;

/**
 * Encapsulates all information about a connected client 
 */
public class Client {
	private Channel channel;
	private CipherChannel cipherChannel;
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
	public CipherChannel getCipherChannel() {
		return cipherChannel;
	}
	public void setCipherChannel(CipherChannel cipherChannel) {
		this.cipherChannel = cipherChannel;
	}
}

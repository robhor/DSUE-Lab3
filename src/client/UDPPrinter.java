package client;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receives data from a DatagramSocket
 * and prints it to stdout
 */
public class UDPPrinter implements Runnable {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final int BUFFERSIZE = 150;
	
	private UDPProtocol protocol;
	private DatagramSocket socket;
	
	public UDPPrinter(DatagramSocket socket) {
		this.socket = socket;
		protocol = new UDPProtocol();
	}

	@Override
	public void run() {
		byte[] buf = new byte[BUFFERSIZE];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		try {
			while(true) {
				socket.receive(packet);
				handleMessage(new String(packet.getData()));
				
				// clear buffer
				java.util.Arrays.fill(buf, (byte) 0);
			}
		} catch (SocketException e) {
			
			return; // Socket has been closed
			
		} catch (IOException e) {
			logger.log(Level.INFO, "IO Error reading from UDP Socket\n" + e);
		}
	}
	
	private void handleMessage(String message) {
		String o = protocol.handleMessage(message);
		if (o != null) System.out.println(o);
	}
	
	public UDPProtocol getProtocol() {
		return protocol;
	}
}

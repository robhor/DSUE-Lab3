package channels;

import java.io.IOException;

public interface Channel {
	/**
	 * Read a line from the channel
	 * @return read line
	 * @throws IOException
	 */
	String read() throws IOException;
	
	/**
	 * Write to the channel
	 * @param message to write
	 */
	void send(String message);
	
	/**
	 * Closes the channel
	 * @throws IOException
	 */
	void close() throws IOException;
}

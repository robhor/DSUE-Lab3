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
	 * @throws IOException
	 */
	void send(String message) throws IOException;
	
	/**
	 * Closes the channel
	 * @throws IOException
	 */
	void close() throws IOException;
}

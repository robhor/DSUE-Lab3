package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PropertyReader {
	private static final Logger logger = Logger.getLogger("PropertyReader");
	
	public static Properties readProperties(String name) {
		InputStream is = ClassLoader.getSystemResourceAsStream(name);
		if (is == null) return null;
		
		Properties props = new Properties();
		try {
			props.load(is);
		} catch (IOException e) {
			return null;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logger.log(Level.WARNING, "IOException reading properties: " + e.getMessage());
			}
		}
		return props;
	}
}

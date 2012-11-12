package billing;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BillingServer extends Remote {
	/**
	 * The access to the billing server is secured by user authentication.
	 * To keep things simple, the username/password combinations can be configured
	 * statically in a config file user.properties. Each line in this file contains
	 * an entry "<username> = <password>", e.g., "john = f23c5f9779a3804d586f4e73178e4ef0".
	 * Do not put plain-text passwords into the config file, but store the MD5 hash
	 * (not very safe either, but sufficient for this assignment) of the passwords.
	 * Use the java.security.MessageDigest class to obtain the MD5 hash of a given password.
	 * If and only if the login information is correct, the management client obtains a reference
	 * to a SecureBillingServer remote object, which performs the actual tasks.
	 * @param username
	 * @param password
	 * @return
	 * @throws RemoteException 
	 */
	BillingServerSecure login(String username, String password) throws RemoteException;
}

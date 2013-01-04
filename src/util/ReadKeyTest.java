package util;

import java.io.FileReader;
import java.io.IOException;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

public class ReadKeyTest {
	public static void main(String[] args) throws IOException {
		FileReader fr = new FileReader("keys/auction-server.pem");
		PasswordFinder pf = new PasswordFinder() {
			public char[] getPassword() { return "23456".toCharArray(); }
		};
		PEMReader pr = new PEMReader(fr, pf);
		
		pr.readObject(); // EXCEPTION
	}
}

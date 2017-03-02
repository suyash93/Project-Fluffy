package gash.router.persistence.replication;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashFunction {

	BigInteger md5hash(String node) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		System.out.println("Inside md5 hash");
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] checksum = md5.digest(node.getBytes("UTF-8"));
		return new BigInteger(1, checksum);
	}

}

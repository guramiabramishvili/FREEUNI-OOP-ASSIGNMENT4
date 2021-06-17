// Cracker.java
/*
 Generates SHA hashes of short strings in parallel.
*/

import java.security.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class Cracker {
	// Array of chars used to produce strings
	public static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz0123456789.,-!".toCharArray();
	
	
	/*
	 Given a byte[] array, produces a hex String,
	 such as "234a6f". with 2 chars for each byte in the array.
	 (provided code)
	*/
	public static String hexToString(byte[] bytes) {
		StringBuffer buff = new StringBuffer();
		for (int i=0; i<bytes.length; i++) {
			int val = bytes[i];
			val = val & 0xff;  // remove higher bits, sign
			if (val<16) buff.append('0'); // leading 0
			buff.append(Integer.toString(val, 16));
		}
		return buff.toString();
	}
	
	/*
	 Given a string of hex byte values such as "24a26f", creates
	 a byte[] array of those values, one byte value -128..127
	 for each 2 chars.
	 (provided code)
	*/
	public static byte[] hexToArray(String hex) {
		byte[] result = new byte[hex.length()/2];
		for (int i=0; i<hex.length(); i+=2) {
			result[i/2] = (byte) Integer.parseInt(hex.substring(i, i+2), 16);
		}
		return result;
	}

	public static String stringToHash(String str) throws NoSuchAlgorithmException {
		String res;
		MessageDigest md = MessageDigest.getInstance("SHA");
		md.update(str.getBytes());
		res = hexToString(md.digest());
		return res;
	}
	private static String result;

	public static void main(String[] args) {
		if (args.length < 1 ) {
			System.out.println("Args: target length [workers]");
			System.exit(1);
		}else if(args.length == 1){
			try {
				System.out.println(stringToHash(args[0]));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			return;
		}else if(args.length != 3){
			System.out.println("Args: target length [workers]");
			System.exit(1);
		}
		// args: targ len [num]
		String targ = args[0];
		int len = Integer.parseInt(args[1]);
		int	numThreads = Integer.parseInt(args[2]);

		Cracker cracker = new Cracker();
		cracker.crackingMode(targ,numThreads,len);
		System.out.println(result);
	}

	private void crackingMode(String hash, int numThreads, int wordLength){
		CountDownLatch latch = new CountDownLatch(1);
		ArrayList<Worker> threads = new ArrayList<Worker>();

		for(int i=0; i<numThreads; i++){
			int numChars = CHARS.length/numThreads;
			threads.add(new Worker(i*numChars,(i+1)*numChars,wordLength,hash,latch));
		}

		for(Thread thread: threads) {
			thread.start();
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for(Thread thread: threads){
			thread.interrupt();
		}

	}

	class Worker extends Thread{
		private final int from;
		private final int to;
		private final int length;
		private final String hash;
		private final CountDownLatch latch;

		public Worker(int from, int to, int length, String hash, CountDownLatch latch){
			this.to = to;
			this.from = from;
			this.length = length;
			this.hash = hash;
			this.latch = latch;
		}
		private void crackHash(int maxLength, String word) {
			if(word.length() > maxLength) return;

			try {
				if (stringToHash(word).equals(hash)) {
					result = word;
					latch.countDown();
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			for (char c : CHARS) {
				if (isInterrupted()){
					return;
				}
				crackHash(maxLength, word + c);
			}
		}

		@Override
		public void run() {
			for(int i = from; i < to; i++) {
				crackHash(length, "" + CHARS[i]);
			}
		}


	}

}

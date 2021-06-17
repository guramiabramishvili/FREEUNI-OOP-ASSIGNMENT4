// Bank.java

/*
 Creates a bunch of accounts and uses threads
 to post transactions to the accounts concurrently.
*/

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Bank {


	public static final int ACCOUNTS = 20;	 // number of accounts
	private static final int BALANCE = 1000;
	private static final int QUEUE_CAP = 40;
	private static final Transaction nullTrans = new Transaction(-1,0,0);

	private int numThreads;
	private static CountDownLatch latch;
	private ArrayList<Account> accounts;
	private ArrayBlockingQueue<Transaction> arrayQueue;



	Bank(int numWorkers){
		numThreads = numWorkers;
		accounts = new ArrayList<Account>();
		for(int i = 0 ; i <ACCOUNTS;i++){
			accounts.add(new Account(this,i,BALANCE));
		}
		arrayQueue = new ArrayBlockingQueue<Transaction>(QUEUE_CAP);
		latch = new CountDownLatch(numWorkers);
	}

	/*
	 Reads transaction data (from/to/amt) from a file for processing.
	 (provided code)
	 */
	public void readFile(String file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			// Use stream tokenizer to get successive words from file
			StreamTokenizer tokenizer = new StreamTokenizer(reader);

			while (true) {
				int read = tokenizer.nextToken();
				if (read == StreamTokenizer.TT_EOF){
					for(int i = 0 ; i < numThreads;i++){
						arrayQueue.put(nullTrans);
					}
					break;  // detect EOF
				}
				int from = (int)tokenizer.nval;

				tokenizer.nextToken();
				int to = (int)tokenizer.nval;

				tokenizer.nextToken();
				int amount = (int)tokenizer.nval;

				// Use the from/to/amount

				arrayQueue.put(new Transaction(from,to,amount));
				arrayQueue.put(new Transaction(to,from,-amount));
			}

		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*
	 Processes one file of transaction data
	 -fork off workers
	 -read file into the buffer
	 -wait for the workers to finish
	*/
	public void processFile(String file, int numWorkers) {

		for(int i = 0 ; i < numWorkers; i++ ){
			Worker w =new Worker();
			w.start();
		}

		readFile(file);

		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class Worker extends Thread{
		@Override
		public void run(){
			try {

				while (true){
					Transaction currTransaction= arrayQueue.take();
					if(currTransaction.equals(nullTrans)){
						latch.countDown();
						break;
					}
					accounts.get(currTransaction.from).makeTransaction(currTransaction);
				}
			}catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}

	/*
	 Looks at commandline args and calls Bank processing.
	*/
	public static void main(String[] args) {
		// deal with command-lines args
		if (args.length == 0) {
			System.out.println("Args: transaction-file [num-workers [limit]]");
			System.exit(1);
		}

		String file = args[0];

		int numWorkers = 1;
		if (args.length >= 2) {
				numWorkers = Integer.parseInt(args[1]);
		}

		Bank myBank= new Bank(numWorkers);
		myBank.processFile(file,numWorkers);

		for(Account acc : myBank.accounts){
			System.out.println(acc);
		}
	}
}

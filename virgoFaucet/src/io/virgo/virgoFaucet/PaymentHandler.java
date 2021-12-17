package io.virgo.virgoFaucet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.crypto.Address;
import io.virgo.virgoAPI.crypto.TransactionBuilder;
import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoCryptoLib.Utils;

public class PaymentHandler implements Runnable {

	private long lastPayment = 0;
	private long delayBetweenPayment = 60000/Main.paymentsPerMinute;
	
	private LinkedBlockingQueue<PaymentRequest> queue = new LinkedBlockingQueue<PaymentRequest>();
	private List<String> addressesInQueue = Collections.synchronizedList(new ArrayList<String>());
	private HashMap<String, Long> locks = new HashMap<String, Long>();
	
	public PaymentHandler() {
		
	}

	@Override
	public void run() {
		
		while(!Thread.interrupted()) {
			try {
				while(Main.faucetBalance < Main.rewardPerPayment)
					Thread.sleep(1000);
				
				PaymentRequest request = queue.take();
				
				if(lastPayment+delayBetweenPayment > System.currentTimeMillis())
					Thread.sleep(Math.max(1, lastPayment+delayBetweenPayment-System.currentTimeMillis()));
				
				try {
					new TransactionBuilder()
					.address(new Address(Main.address))
					.output(new TxOutput(request.getAddress(), Main.rewardPerPayment))
					.send(Main.privateKey);
					
					Main.faucetBalance -= Main.rewardPerPayment;
					addressesInQueue.remove(request.getAddress());
					lastPayment = System.currentTimeMillis();
				} catch (Exception e) {
					queue.add(request);
				}
				
			} catch (InterruptedException e) {}
		}
		
	}
	
	public int queue(PaymentRequest request) {
		if(addressesInQueue.contains(request.getAddress()))
			return 1;
		
		if(!Utils.validateAddress(request.getAddress(), VirgoAPI.ADDR_IDENTIFIER))
			return 2;
		
		if(locks.containsKey(request.getAddress()) && locks.get(request.getAddress()) > System.currentTimeMillis())
			return 3;
		
		queue.add(request);
		addressesInQueue.add(request.getAddress());
		locks.put(request.getAddress(), System.currentTimeMillis()+Main.timeBetweenClaims);
		
		return 0;
	}
	
}

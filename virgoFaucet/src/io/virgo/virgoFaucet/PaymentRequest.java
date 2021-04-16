package io.virgo.virgoFaucet;

public class PaymentRequest {

	private String address;
	private long date;
	
	public PaymentRequest(String address, long date) {
		this.address = address;
		this.date = date;
	}
	
	public String getAddress() {
		return address;
	}
	
	public long getSubmissionDate() {
		return date;
	}
	
}

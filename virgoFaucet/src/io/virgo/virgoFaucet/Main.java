package io.virgo.virgoFaucet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoAPI.requestsResponses.GetBalancesResponse;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.ECDSA;

public class Main {
	
	public static VirgoAPI api;
	
	public static int rewardPerPayment = 500000000;
	
	public static long timeBetweenClaims = 60 * 60000;
	
	public static byte[] privateKey;
	public static String address;
	
	public static int paymentsPerMinute = 10;
 	
	public static long faucetBalance = 0;
	
	public static PaymentHandler handler;
	
	private static ArrayList<String> providersList = new ArrayList<String>(Arrays.asList("https://us.eagle.virgo.network:8000/", "https://ap.eagle.virgo.network:8000/", "https://eu.eagle.virgo.network:8000/"));
	
	public static String hCaptchaSecretKey = "";
	public static String hCaptchaSitekey = "";
	
	public static void main(String[] args) throws IOException {
				
		try {
			setup();
		} catch(IOException e) {
			System.out.println("Cannot access config.json, please check permissions. Aborting.");
		}
		
		address = Converter.Addressify(ECDSA.getPublicKey(privateKey), VirgoAPI.ADDR_IDENTIFIER);
		
		api = new VirgoAPI.Builder().build();
		
		for(String provider : providersList)
			api.addProvider(new URL(provider));
		
		new Server();
		
		handler = new PaymentHandler();
		new Thread(handler).start();
		
		//update faucet balance every 10 seconds
		new Timer().scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				GetBalancesResponse resp = api.getBalances(new String[] {address});
				
				if(resp.getResponseCode() != ResponseCode.OK)
					return;
				
				faucetBalance = resp.getBalance(address).getFinalBalance();
			}
			
		}, 10000l, 1000l);
		
	}
	
	private static void setup() throws IOException {

		File configFile = new File("config.json");
		
		if(!configFile.exists())
			writeBaseConfig(configFile);
		
		if(!configFile.isFile()) {
			System.out.println("config.json is not a file, please suppress it. aborting.");
			System.exit(0);
			return;
		}
		
		String configText = new String(Files.readAllBytes(configFile.toPath()));
		
		try {
			JSONObject config = new JSONObject(configText);
			
			privateKey = Converter.hexToBytes(config.getString("privateKey"));
			rewardPerPayment = config.getInt("rewardPerPayment");
			paymentsPerMinute = config.getInt("paymentsPerMinute");
			hCaptchaSecretKey = config.getString("hCaptchaSecretKey");
			hCaptchaSitekey = config.getString("hCaptchaSitekey");
			
			JSONArray providers = config.getJSONArray("providers");
			for(int i = 0; i< providers.length(); i++)
				providersList.add(providers.getString(i));
			
		}catch(JSONException|IllegalArgumentException e) {
			writeBaseConfig(configFile);
		}
		
	}
	
	private static void writeBaseConfig(File configFile) throws IOException {
		System.out.println("Config file doesn't exists or invalid, creating one");
		JSONObject baseConfig = new JSONObject();
		baseConfig.put("privateKey", "");
		baseConfig.put("rewardPerPayment", Main.rewardPerPayment);
		baseConfig.put("paymentsPerMinute", Main.paymentsPerMinute);
		baseConfig.put("hCaptchaSecretKey", Main.hCaptchaSecretKey);
		baseConfig.put("hCaptchaSitekey", Main.hCaptchaSitekey);
		
		JSONArray providers = new JSONArray(providersList);
		baseConfig.put("providers", providers);
		
		Files.writeString(configFile.toPath(), baseConfig.toString());
		System.out.println("config.json created, please edit it to provide faucet's wallet private key.");
		
		System.exit(0);
		return;
	}

	public static VirgoAPI getAPI() {
		return api;
	}

}

package io.virgo.virgoFaucet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;

import com.sun.net.httpserver.HttpServer;

public class Server {

	public Server() throws IOException {
		
		HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
		server.createContext("/", (exchange -> {
			
			String requestedResourceName = "index.html";
			String[] path = exchange.getRequestURI().toString().substring(1).split("\\?", 2);
			
			String[] rawArgs = path[0].split("/");
			
        	if(rawArgs.length > 0 && !rawArgs[0].equals(""))
        		requestedResourceName = rawArgs[0].replace("../", "");

        	HashMap<String, String> params = new HashMap<String, String>();
        	if(path.length == 2)
        		params.putAll(parseParams(path[1]));

        	InputStream resource = Main.class.getResourceAsStream("/src/main/resources/"+requestedResourceName);
        	
        	if(resource != null) {
        		byte[] response = resource.readAllBytes();
        		
        		if(requestedResourceName.equals("index.html")) {
        			
        			String responseText = new String(response);
        			responseText = responseText
        					.replace("{faucetAddress}", Main.address)
        					.replace("{vgoPerSend}", Miscellaneous.formatLong(Main.rewardPerPayment))
        					.replace("{faucetBalance}", Miscellaneous.formatLong(Main.faucetBalance));
        			
        			String actionMessage = "";
        			
        			if(params.containsKey("address") && !params.get("address").equals(Main.address))	
        				switch(Main.handler.queue(new PaymentRequest(params.get("address"), System.currentTimeMillis()))) {
        				case 0:
            				actionMessage = "<span class='textSuccess'>You've been added to the queue and will receive your coins soon !</span>";
            				break;
            				
        				case 1:
        					actionMessage = "<span class='textError'>You are already in queue.</span>";
        					break;
        					
        				case 2:
        					actionMessage = "<span class='textError'>Invalid address !</span>";
        					break;
        				}
        				
    				responseText = responseText.replace("{actionMessage}", actionMessage);
        			
        			response = responseText.getBytes();
        		}
        		
    			exchange.sendResponseHeaders(200, response.length);
                OutputStream output = exchange.getResponseBody();
                output.write(response);
                output.flush();
        	} else {
    			exchange.sendResponseHeaders(404, 0);
        	}

            exchange.close();
		}));
		
		server.start();
		
	}
	
	private HashMap<String, String> parseParams(String paramsString){
		HashMap<String, String> paramsMap = new HashMap<String, String>();
		
		String[] params = paramsString.split("&");
		
		for(String paramString : params) {
			String[] param = paramString.split("=", 2);
			if(param.length == 2)
				paramsMap.put(param[0], param[1]);
		}
		
		return paramsMap;
	}
	
}

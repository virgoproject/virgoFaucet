package io.virgo.virgoFaucet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.HashMap;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpServer;

public class Server {

    HttpClient httpClient = null;
	
	public Server() throws IOException {
		
		boolean hCaptchaEnabled = !Main.hCaptchaSecretKey.equals("") && !Main.hCaptchaSitekey.equals("");
		
	    if(hCaptchaEnabled)
	    	httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .build();
	    
		
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
        			
        			
        			if(hCaptchaEnabled) {
        				responseText = responseText.replace("{hCaptchaJS}", "<script src=\"https://hcaptcha.com/1/api.js\" async defer></script>")
        						.replace("{hCaptchaBox}", "<br><div class=\"h-captcha\" data-sitekey=\""+ Main.hCaptchaSitekey +"\"></div><br>");
        			}
        				
        			String actionMessage = "";
        			
        			if(params.containsKey("address") && !params.get("address").equals(Main.address)) {
        				
        				boolean proceed = true;
        				
        				if(hCaptchaEnabled) {
        					if(params.containsKey("h-captcha-response")) {
        						
        						System.out.println(params.get("h-captcha-response"));        						
	    						var sb = new StringBuilder();
	    						sb.append("response=");
	    						sb.append(params.get("h-captcha-response"));
	    						sb.append("&secret=");
	    						sb.append(Main.hCaptchaSecretKey);
	
	    						HttpRequest captchaVerifReq = HttpRequest.newBuilder()
						          .uri(URI.create("https://hcaptcha.com/siteverify"))
						          .header("Content-Type", "application/x-www-form-urlencoded")
						          .timeout(Duration.ofSeconds(10))
						          .POST(BodyPublishers.ofString(sb.toString())).build();
	
								try {
									HttpResponse<String> verifResp = httpClient.send(captchaVerifReq,
									  BodyHandlers.ofString());
																		
									if(verifResp.statusCode() == 200) {
										JSONObject respJSON = new JSONObject(verifResp.body());
										if(!respJSON.getBoolean("success")) {
				        					actionMessage = "<span class='textError'>Please resolve the captcha.</span>";
											proceed = false;
										}
									}else {
			        					actionMessage = "<span class='textError'>Internal error, please try again later.</span>";
			        					proceed = false;
									}
											    						
								} catch (Exception e) {
		        					actionMessage = "<span class='textError'>Internal error, please try again later.</span>";
									proceed = false;
								}
        						
        					}else {
	        					actionMessage = "<span class='textError'>Please resolve the captcha.</span>";
        						proceed = false;
        					}
        				}
        				
        				if(proceed)
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

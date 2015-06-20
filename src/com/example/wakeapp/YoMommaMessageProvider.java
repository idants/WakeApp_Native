package com.example.wakeapp;

import org.json.JSONObject;

public class YoMommaMessageProvider extends messageProvider {

	public YoMommaMessageProvider() {
		URL = "http://api.yomomma.info/";
	}
	
	@Override
	public String getMessage(String messageProviderMessage) {
		try {
			final JSONObject data = new JSONObject(messageProviderMessage);
			final String joke = data.getString("joke");
        
	        return joke;
		}
		catch(Exception e) {
			return e.getMessage();
		}
	}

}

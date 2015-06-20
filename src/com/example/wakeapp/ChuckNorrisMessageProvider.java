package com.example.wakeapp;

import org.json.JSONObject;

public class ChuckNorrisMessageProvider extends messageProvider {

	public ChuckNorrisMessageProvider() {
		URL = "http://api.icndb.com/jokes/random";
	}
	
	@Override
	public String getMessage(String messageProviderMessage) {
		try {
			final JSONObject data = new JSONObject(messageProviderMessage);
			final JSONObject value = data.getJSONObject("value");
			String joke = value.getString("joke");
	        
	        return joke;
		}
		catch(Exception e) {
			return e.getMessage();
		}
	}
}

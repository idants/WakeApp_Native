package com.example.wakeapp;

import java.util.Iterator;
import org.json.JSONObject;

public class WikiMessageProvider extends messageProvider {
	public WikiMessageProvider() {
		URL = "http://en.wikipedia.org/w/api.php?action=query&generator=random&grnnamespace=0&prop=extracts&exchars=150&format=json&continue=";
	}
	
	@Override
	public String getMessage(String messageProviderMessage) {
		try {
			final JSONObject data = new JSONObject(messageProviderMessage);
			final JSONObject query = data.getJSONObject("query");
			final JSONObject pages = query.getJSONObject("pages");
			Iterator<?> keys = pages.keys();

            String key = (String)keys.next();
            JSONObject page = pages.getJSONObject(key);
            String title = page.getString("title");
            
	        String extract = page.getString("extract");
	        String message = extract.replace("</p>...", "");
	        int endIndex = 137 - title.length();
	        message = message.substring(0, endIndex) + "...</p>";
	        int pageid = page.getInt("pageid");
	        
	        return message + "<br><a href=\"http://en.wikipedia.org/wiki?curid=" + pageid + "\">" + title + "</a>";
		}
		catch(Exception e) {
			return e.getMessage();
		}
	}
}

package com.example.wakeapp;

public class QuotesMessageProvider extends messageProvider {
	public QuotesMessageProvider() {
		URL = "http://www.iheartquotes.com/api/v1/random";
	}
	
	@Override
	public String getMessage(String messageProviderMessage) {
		String[] lines = messageProviderMessage.split("\n");
		String message = "";
		for (int i=0; i<lines.length-2; i++){
			message += lines[i] + "\n";
		}
		return message;
	}

}

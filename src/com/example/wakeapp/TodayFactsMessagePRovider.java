package com.example.wakeapp;

import java.util.Calendar;

public class TodayFactsMessagePRovider extends messageProvider {

	public TodayFactsMessagePRovider() {
		Calendar c = Calendar.getInstance(); 
		int month = c.get(Calendar.MONTH) + 1;
		int date = c.get(Calendar.DATE);
		URL = "http://numbersapi.com/" + month + "/" + date + "/date";
		name = "Fact";
	}
	
	@Override
	public String getMessage(String messageProviderMessage) {
		String message = messageProviderMessage;
		return name + ":\n" + message;
	}
}

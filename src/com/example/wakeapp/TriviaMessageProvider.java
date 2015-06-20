package com.example.wakeapp;

public class TriviaMessageProvider extends messageProvider {
	public TriviaMessageProvider() {
		URL = "http://numbersapi.com/random/trivia";
		name = "Fact";
	}
	
	@Override
	public String getMessage(String messageProviderMessage) {
		return name + ":\n" + messageProviderMessage;
	}
}

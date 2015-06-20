package com.example.wakeapp;

//add a config XML file to hold all provider parameters
public abstract class messageProvider {
	public String URL = "";
	public String name = "";
	public abstract String getMessage(String messageProviderMessage); 
}

package com.example.wakeapp;

import java.util.Random;

import android.content.Context;

public class BackupMessageProvider extends messageProvider{

	private static String messages[] = new String[13];
	
	public BackupMessageProvider(Context context){
		URL = "";
		int i=0;
		int id = context.getResources().getIdentifier("backupMessage" + i, "string", "com.example.wakeapp");
		while(id != 0){
			messages[i++] = (String) context.getResources().getText(id);
			id = context.getResources().getIdentifier("backupMessage" + i, "string", "com.example.wakeapp");
		}
	}
	
	@Override
	public String getMessage(String messageProviderMessage) {
		int messageIndex = new Random().nextInt(messages.length);
		return messages[messageIndex];
	}
}

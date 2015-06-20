package com.example.wakeapp;

import com.facebook.AppEventsLogger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class ConfirmActivity extends Activity {
	private int hours;
	private int minutes;
	private boolean alarmAlreadySet;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_confirm);
		
		// Get the message from the intent
	    Intent intent = getIntent();
	    hours = intent.getIntExtra(TimeActivity.EXTRA_MESSAGE_HOURS, 0);
	    minutes = intent.getIntExtra(TimeActivity.EXTRA_MESSAGE_MINUTES, 0);
	    alarmAlreadySet = intent.getBooleanExtra(TimeActivity.EXTRA_MESSAGE_ALARM_ALREADY_SET, false);

	    // Create the text view
	    TextView textView = (TextView) findViewById(R.id.tConfirm);
	    String message = "Alarm was set:\n" + (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes;
	    
	    textView.setText(message);
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
	    return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.message, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		// Logs 'app activate' App Event.
		AppEventsLogger.activateApp(this);		
	}
	
	@Override
	protected void onPause() {
	  super.onPause();

	  // Logs 'app deactivate' App Event.
	  AppEventsLogger.deactivateApp(this);
	}
	
	public void goToHome(View view){
		final Intent intent = new Intent(this, TimeActivity.class);
		intent.putExtra(TimeActivity.EXTRA_MESSAGE_HOURS, hours);
		intent.putExtra(TimeActivity.EXTRA_MESSAGE_MINUTES, minutes);
		intent.putExtra(TimeActivity.EXTRA_MESSAGE_ALARM_ALREADY_SET, alarmAlreadySet);
		startActivity(intent);
		
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}
}

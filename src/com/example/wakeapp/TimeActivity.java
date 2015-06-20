package com.example.wakeapp;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.AppEventsLogger;

public class TimeActivity extends Activity {

	public static int hours;
	public static int minutes;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private EditText etHours;
	private EditText etMinutes;
	private double longitude = 0;
	private double latitude = 0;
	private messageProvider chosenProvider = null;
	private messageProvider backupProvider = null;
	private String APIMessage;
	private boolean alarmAlreadySet = false;

	private EditTextLocker editTextLockerHours;
	private EditTextLocker editTextLockerMinutes;
	
	private final static int MAIN_PROVIDER_WEIGHT = 3;
	
	GPSTracker gps;
	
	public final static String EXTRA_MESSAGE_HOURS = "com.example.wakeapp.MESSAGE_HOURS";
	public final static String EXTRA_MESSAGE_MINUTES = "com.example.wakeapp.MESSAGE_MINUTES";
	public final static String EXTRA_MESSAGE_ALARM_ALREADY_SET = "com.example.wakeapp.MESSAGE_ALARM_ALREADY_SET";
	public final static String EXTRA_MESSAGE_LATITUDE = "com.example.wakeapp.MESSAGE_LATITUDE";
	public final static String EXTRA_MESSAGE_LONGITUDE = "com.example.wakeapp.MESSAGE_LONGITUDE";
	public final static String EXTRA_MESSAGE_APIMESSAGE = "com.example.wakeapp.MESSAGE_APIMESSAGE";
	
	public final static int DAY_IN_SECONDS = 24 * 60 * 60;
	public final static int HOUR_IN_SECONDS = 60 * 60;

	private void getAPIMessage() {
		messageProvider[] mainMessageProviders = {
				new QuotesMessageProvider(),
				new WikiMessageProvider(),
				new ChuckNorrisMessageProvider(),
				new YoMommaMessageProvider(),
				new TodayFactsMessagePRovider(),
				new TriviaMessageProvider(),
				new WeatherMessageProvider(latitude, longitude)
		};

		messageProvider[] messageProviders = new messageProvider[mainMessageProviders.length * MAIN_PROVIDER_WEIGHT + 1];
		
		backupProvider = new BackupMessageProvider(this);
		
		int k=0;
		messageProviders[k++] = backupProvider;
		for (int i = 0; i < mainMessageProviders.length; i++){
			for (int j = 0; j < MAIN_PROVIDER_WEIGHT; j++){
				messageProviders[k++] = mainMessageProviders[i];
			}
		}

		// randomly choose a message provider
		int weatherProviderIndex = mainMessageProviders.length - 1;
		int weatherProviderIndexInMessageProviders = MAIN_PROVIDER_WEIGHT * weatherProviderIndex + 1;
		int providerIndex = new Random().nextInt(messageProviders.length);
		
		// if there's no data from GPS remove weather message provider from options 
		if ((providerIndex == weatherProviderIndexInMessageProviders || providerIndex == weatherProviderIndexInMessageProviders + 1 || providerIndex == weatherProviderIndexInMessageProviders + 2) 
				&& longitude == 0 && latitude == 0) {
			System.out.println("GPS not available, remove Weather provider from list");
			providerIndex = new Random().nextInt(messageProviders.length - MAIN_PROVIDER_WEIGHT);
		}
		chosenProvider = messageProviders[providerIndex];
		if (chosenProvider == null){
			System.out.println("No message provider found, switching to backup");
			chosenProvider = backupProvider;
		}
		
		System.out.println("Chosen provider: " + chosenProvider);
		
		if (chosenProvider.URL != ""){
			ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
			if (networkInfo != null && networkInfo.isConnected()) {
				new DownloadWebpageTask().execute(chosenProvider.URL);
			} else {
				System.out.println("Network not available, switching to backup...");
				APIMessage = backupProvider.getMessage("").trim();
			}	
		}
		else{
			APIMessage = backupProvider.getMessage("").trim();
		}
		
	}

	// Uses AsyncTask to create a task away from the main UI thread. This task
	// takes a URL string and uses it to create an HttpUrlConnection. Once the
	// connection has been established, the AsyncTask downloads the contents of the webpage
	// as an InputStream. Finally, the InputStream is converted into a string,
	// which is displayed in the UI by the AsyncTask's onPostExecute method.
	private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {

			// params comes from the execute() call: params[0] is the url.
			try {
				return downloadUrl(urls[0]);
			} catch (IOException e) {
				return "RC_INVALID_URL";
			}
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String message) {
			if (message == "RC_INVALID_URL"){
				APIMessage = backupProvider.getMessage("").trim();
			}
			else{
				String output = chosenProvider.getMessage(message).trim();
				APIMessage = output;	
			}
		}
	}

	// Given a URL, establishes an HttpUrlConnection and retrieves
	// the web page content as a InputStream, which it returns as
	// a string.
	private String downloadUrl(String myurl) throws IOException {
		InputStream is = null;
		// Only display the first 500 characters of the retrieved
		// web page content.
		int len = 500;

		try {
			URL url = new URL(myurl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the query
			conn.connect();
			int response = conn.getResponseCode();
			Log.d("Wakeapp", "The response is: " + response);
			is = conn.getInputStream();
			// Convert the InputStream into a string
			String contentAsString = readIt(is, len);
			return contentAsString;

			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	// Reads an InputStream and converts it to a String.
	private String readIt(InputStream stream, int len) throws IOException,
			UnsupportedEncodingException {
		Reader reader = null;
		reader = new InputStreamReader(stream, "UTF-8");
		char[] buffer = new char[len];
		reader.read(buffer);
		return new String(buffer);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//this is to address a bug where after app is launched, launching it again starts the main activity again instead of resuming the last activity
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {  
	        // Activity was brought to front and not created, 
	        // Thus finishing this will get us to the last viewed activity 
	        finish(); 
	        return; 
	    }
		
		setContentView(R.layout.activity_time);
		Typeface setAlarmFont = Typeface.createFromAsset(getAssets(),
				"fonts/Fabrica.otf");
		Button buttonSetAlarm = (Button) findViewById(R.id.button_setAlarm);
		buttonSetAlarm.setTypeface(setAlarmFont, Typeface.BOLD);

		etHours = (EditText) findViewById(R.id.edit_hours);
		etMinutes = (EditText) findViewById(R.id.edit_minutes);

		// Get the message from the intent
		Intent intent = getIntent();
		hours = intent.getIntExtra(TimeActivity.EXTRA_MESSAGE_HOURS, 0);
		minutes = intent.getIntExtra(TimeActivity.EXTRA_MESSAGE_MINUTES, 0);
		alarmAlreadySet = intent.getBooleanExtra(TimeActivity.EXTRA_MESSAGE_ALARM_ALREADY_SET, false);

		if (alarmAlreadySet) { // if at least one has a value update
			etHours.setText((hours < 10 ? "0" : "") + Integer.toString(hours));
			etMinutes.setText((minutes < 10 ? "0" : "") + Integer.toString(minutes));
			toggleResetVisibility(View.VISIBLE);
		}

		editTextLockerHours = new EditTextLocker(etHours, 0, 23, etMinutes);
		editTextLockerHours.limitCharacters(2);
		editTextLockerMinutes = new EditTextLocker(etMinutes, 0, 59, null);
		editTextLockerMinutes.limitCharacters(2);
		
		etMinutes.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		    	if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		            setAlarm(v);
		            return true;
		        }
		    	
		    	if (keyCode == KeyEvent.KEYCODE_DEL) {
		    		editTextLockerMinutes.startStopEditing(false);
	            }
		        return false;
		    }
	    });
		
		etHours.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		    	if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		    		etMinutes.requestFocus();
		            return true;
		        }
		    	
		    	if (keyCode == KeyEvent.KEYCODE_DEL) {
		    		editTextLockerHours.startStopEditing(false);
	            }
		        return false;
		    }
	    });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.time, menu);
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
		if (Globals.wakelock != null && !Globals.wakelock.isHeld()){
			Globals.wakelock.acquire();	
		}
		
		if (alarmAlreadySet) { // if at least one has a value update
			toggleResetVisibility(View.VISIBLE);
		}
		
		// Logs 'install' and 'app activate' App Events.
		AppEventsLogger.activateApp(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (Globals.wakelock != null && Globals.wakelock.isHeld()){
			Globals.wakelock.release();	
		}
		
		// Logs 'app deactivate' App Event.
		AppEventsLogger.deactivateApp(this);
	}
	
	@Override
	public void onBackPressed() {
		// do nothing as we want to prevent back behavior on this activity
		//super.onBackPressed();
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
	    return false;
	}
	

	/**
	 * saves the alarm
	 * 
	 * @param view
	 */
	public void setAlarm(View view) {
		EditText editHours = (EditText) findViewById(R.id.edit_hours);
		EditText editMinutes = (EditText) findViewById(R.id.edit_minutes);
		Editable eHours = editHours.getText();
		Editable eMinutes = editMinutes.getText();

		if (eHours != null && eMinutes != null) {
			String sHours = eHours.toString();
			String sMinutes = eMinutes.toString();

			hours = sHours.length() == 0 ? 0 : Integer.parseInt(sHours, 10);
			minutes = sMinutes.length() == 0 ? 0 : Integer.parseInt(sMinutes, 10);

			Time now = new Time();
			now.setToNow();

			int currentHours = now.hour;
			int currentMinutes = now.minute;
			int currentSeconds = now.second;

			int delayUntilWakeup = ((hours - currentHours) * HOUR_IN_SECONDS)
					+ ((minutes - currentMinutes) * 60) - currentSeconds;
			if (delayUntilWakeup < 0) {
				delayUntilWakeup += DAY_IN_SECONDS;
			}

			final Activity context = this;
			final Runnable waker = new Runnable() {
				public void run() {
					System.out.println("Starting wake activity");
					Intent wakeIntent = new Intent("intent.wake.action");
					wakeIntent.setComponent(new ComponentName(context
							.getPackageName(), WakeActivity.class.getName()));
					wakeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					wakeIntent.putExtra(EXTRA_MESSAGE_LATITUDE, latitude);
					wakeIntent.putExtra(EXTRA_MESSAGE_LONGITUDE, longitude);
					if (APIMessage == null || APIMessage.length() == 0){
						System.out.println("API message not ready, switching to backup");
						APIMessage = backupProvider.getMessage("").trim();
					}
					wakeIntent.putExtra(EXTRA_MESSAGE_APIMESSAGE, APIMessage);
					
					PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
					Globals.wakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "WakeAppTag");
					
					KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE); 
					Globals.keyguardLock = km.newKeyguardLock("WakeAppKeyguardLock"); 
					Globals.keyguardLock.disableKeyguard(); 
					
					if (!Globals.wakelock.isHeld()){
						Globals.wakelock.acquire();	
					}
					
					context.getApplication().startActivity(wakeIntent);
					finish();
				};
			};

			findCurrentLocation();
			
			final Runnable APIMessageGetter = new Runnable() {
				public void run() {
					getAPIMessage();
				};
			};

			if (alarmAlreadySet) {
				resetHelper();
			}

			int delayUntilLocationUpdate = delayUntilWakeup < 300 ? 0
					: (delayUntilWakeup - 300);
			
			ScheduledFuture<?> wakerFuture = scheduler.schedule(waker,
					delayUntilWakeup, SECONDS);
			ScheduledFuture<?> APIMessageFuture = scheduler.schedule(
					APIMessageGetter, delayUntilLocationUpdate + 5, SECONDS);

			System.out.println("Delay until wakeapp is " + delayUntilWakeup);
			System.out.println("Delay until location update is " + delayUntilLocationUpdate);
			
			ScheduledTasks.tasks.put("waker", wakerFuture);
			ScheduledTasks.tasks.put("API", APIMessageFuture);

			alarmAlreadySet = true;
			
			Intent intent = new Intent(this, ConfirmActivity.class);
			intent.putExtra(EXTRA_MESSAGE_HOURS, hours);
			intent.putExtra(EXTRA_MESSAGE_MINUTES, minutes);
			intent.putExtra(EXTRA_MESSAGE_ALARM_ALREADY_SET, true);
			startActivity(intent);
		}
	}

	/**
	 * resets the alarm
	 * 
	 * @param view
	 */
	public void resetAlarm(View view) {
		resetHelper();
		alarmAlreadySet = false;
		toggleResetVisibility(View.INVISIBLE);

		EditText editHours = (EditText) findViewById(R.id.edit_hours);
		editHours.setText("");
		EditText editMinutes = (EditText) findViewById(R.id.edit_minutes);
		editMinutes.setText("");
		editTextLockerHours.startStopEditing(false);
		editTextLockerMinutes.startStopEditing(false);
		editHours.requestFocus();
	}

	private void toggleResetVisibility(int visibility) {
		ImageButton ibResetAlarm = (ImageButton) findViewById(R.id.ib_resetAlarm);
		ibResetAlarm.setVisibility(visibility);
		TextView tExistingAlarm = (TextView) findViewById(R.id.tExistingAlarm);
		tExistingAlarm.setVisibility(visibility);
	}

	private void resetHelper() {
		ScheduledFuture<?> wakerFuture;
		ScheduledFuture<?> locationFuture;
		ScheduledFuture<?> APIMessageFuture;

		wakerFuture = ScheduledTasks.tasks.get("waker");
		locationFuture = ScheduledTasks.tasks.get("location");
		APIMessageFuture = ScheduledTasks.tasks.get("API");

		if (wakerFuture != null) {
			wakerFuture.cancel(false);
			ScheduledTasks.tasks.remove("waker");
		}

		if (locationFuture != null) {
			locationFuture.cancel(false);
			ScheduledTasks.tasks.remove("location");
		}

		if (APIMessageFuture != null) {
			APIMessageFuture.cancel(false);
			ScheduledTasks.tasks.remove("API");
		}
	}

	private void findCurrentLocation() {
		gps = new GPSTracker(TimeActivity.this);
		
		if (gps.canGetLocation()){
			latitude = gps.getLatitude();
			longitude = gps.getLongitude();
		}
	}
	

}

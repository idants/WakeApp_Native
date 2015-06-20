package com.example.wakeapp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import com.facebook.AppEventsLogger;

import static java.util.concurrent.TimeUnit.*;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class WakeActivity extends Activity {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private double latitude;
	private double longitude;
	private String APIMessage;
	private MediaPlayer mp;
	private int oldConfigInt;
	private Vibrator vibrator;
	private long[] vibrationPattern = {0, 2000, 1000}; // {<delay to start>, <vibrate duration>, <sleep between vibrations>}
	private final int snoozeTimeout = 300; //in seconds

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Wake activity started");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wake);
		
		// Get the message from the intent
	    Intent intent = getIntent();
	    latitude = intent.getDoubleExtra(TimeActivity.EXTRA_MESSAGE_LATITUDE, 0);
	    longitude = intent.getDoubleExtra(TimeActivity.EXTRA_MESSAGE_LONGITUDE, 0);
	    APIMessage = intent.getStringExtra(TimeActivity.EXTRA_MESSAGE_APIMESSAGE);
		
		Typeface wakeButtonsFont = Typeface.createFromAsset(getAssets(), "fonts/brushstr.ttf");
		Button buttonSnooze = (Button)findViewById(R.id.tSnooze);
		buttonSnooze.setTypeface(wakeButtonsFont);
		Button buttonWake = (Button)findViewById(R.id.tWake);
		buttonWake.setTypeface(wakeButtonsFont);
		
		if ( (oldConfigInt & ActivityInfo.CONFIG_ORIENTATION) != ActivityInfo.CONFIG_ORIENTATION ) { // initial call, not on rotation
			/*Intent receiverIntent = new Intent(this, ReceiverForAlarm.class);
			receiverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 12345, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);*/
			
			// Vibrate the mobile phone
	        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        vibrator.vibrate(vibrationPattern, 0);
			
			Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
			
			mp = MediaPlayer.create(this.getApplicationContext(), alert);
			if (mp != null){
				mp.setVolume(1.0f, 1.0f);
				mp.start();
				mp.setLooping(true);	
			}
			
	    	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    	Globals.wakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "WakeAppTag");

			KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE); 
			Globals.keyguardLock = km.newKeyguardLock("WakeAppKeyguardLock"); 
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu) {
	    return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wake, menu);
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
		System.out.println("wake onResume");
		if (mp != null && !mp.isPlaying()) {
			mp.setVolume(1.0f, 1.0f);
			mp.start();
			mp.setLooping(true);
	    }
		vibrator.vibrate(vibrationPattern, 0);
		
		if (Globals.wakelock != null && !Globals.wakelock.isHeld()){
			Globals.wakelock.acquire();
		}
		super.onResume();
		// Logs 'app activate' App Event.
		AppEventsLogger.activateApp(this);
	}
	
	@Override
	protected void onPause() {
		System.out.println("wake onPause");
        if (mp != null && mp.isPlaying()) {
            mp.stop();
        }
        vibrator.cancel();
	    super.onPause();

	    if (Globals.wakelock != null && Globals.wakelock.isHeld()){
	    	Globals.wakelock.release();
	    }
        // Logs 'app deactivate' App Event.
		AppEventsLogger.deactivateApp(this);
	}
	
	@Override
	protected void onDestroy() {
		System.out.println("wake onDestroy");
		if (mp != null && mp.isPlaying()) {
            mp.stop();
        }
		vibrator.cancel();
		super.onDestroy();
		oldConfigInt = getChangingConfigurations();
		// Logs 'app deactivate' App Event.
		AppEventsLogger.deactivateApp(this);
	}
	
	@Override
	public void onBackPressed() {
		// do nothing as we want to prevent back behavior on this activity
		//super.onBackPressed();
	}
	
	public void snooze(View view) {
		if (mp != null && mp.isPlaying()) {
            mp.stop();
        }
		vibrator.cancel();
		if (Globals.wakelock != null && Globals.wakelock.isHeld()){
			Globals.wakelock.release();
		}
        int delayUntilWakeup = snoozeTimeout;
		
        final Activity context = this;
		final Runnable waker = new Runnable() {
		    public void run() { 
		    	System.out.println("back from snooze!");
		    	if (Globals.keyguardLock != null){
		    		Globals.keyguardLock.disableKeyguard();	
		    	}
				
		    	if (Globals.wakelock != null && !Globals.wakelock.isHeld()){
		    		Globals.wakelock.acquire();
		    	}

		    	Intent intent = new Intent("intent.wake.action");
		    	intent.setComponent(new ComponentName(context
						.getPackageName(), WakeActivity.class.getName()));
		    	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				
				intent.putExtra(TimeActivity.EXTRA_MESSAGE_LATITUDE, latitude);
				intent.putExtra(TimeActivity.EXTRA_MESSAGE_LONGITUDE, longitude);
				intent.putExtra(TimeActivity.EXTRA_MESSAGE_APIMESSAGE, APIMessage);
		    	
				context.getApplication().startActivity(intent);
				finish();
		    };
		};
		
		ScheduledFuture<?> wakerFuture = scheduler.schedule(waker, delayUntilWakeup, SECONDS);
		ScheduledTasks.tasks.put("waker", wakerFuture);
		
		Time now = new Time();
		now.setToNow();

		int currentHours = now.hour;
		int currentMinutes = now.minute;
		
		//recalculate time after snooze
		if (currentMinutes >= 55){
			currentHours = (currentHours + 1) % 24;
		}
		currentMinutes = (currentMinutes + 5) % 60; 
		
		final Intent timeIntent = new Intent(this, TimeActivity.class);
		timeIntent.putExtra(TimeActivity.EXTRA_MESSAGE_HOURS, currentHours);
		timeIntent.putExtra(TimeActivity.EXTRA_MESSAGE_MINUTES, currentMinutes);
		timeIntent.putExtra(TimeActivity.EXTRA_MESSAGE_ALARM_ALREADY_SET, true);
		startActivity(timeIntent);
		
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
	}
	
	public void wake(View view) {
		if (mp != null && mp.isPlaying()) {
            mp.stop();
        }
		vibrator.cancel();

		final Intent intent = new Intent(this, MessageActivity.class);
		intent.putExtra(TimeActivity.EXTRA_MESSAGE_LATITUDE, latitude);
		intent.putExtra(TimeActivity.EXTRA_MESSAGE_LONGITUDE, longitude);
		intent.putExtra(TimeActivity.EXTRA_MESSAGE_APIMESSAGE, APIMessage);
		startActivity(intent);
	}
}

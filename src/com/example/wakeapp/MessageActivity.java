package com.example.wakeapp;

import com.facebook.AppEventsLogger;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MessageActivity extends Activity {

	private TextView tMessage;
	private String APIMessage;
	private UiLifecycleHelper uiHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		System.out.println("Message activity started");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message);
		uiHelper = new UiLifecycleHelper(this, null);
	    uiHelper.onCreate(savedInstanceState);

		// Get the message from the intent
	    Intent intent = getIntent();
	    APIMessage = intent.getStringExtra(TimeActivity.EXTRA_MESSAGE_APIMESSAGE);
	    if (APIMessage == null){
	    	APIMessage = "";
	    }
	    
	    final String DOUBLE_BYTE_SPACE = "\u3000";
	    tMessage = (TextView) findViewById(R.id.tMessage);
	    
	    // bug fix for http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within-bounds/21851157#21851157
	    String fixString = "";
	    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1
	       && android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {  
	        fixString = DOUBLE_BYTE_SPACE;
	    }
    
	    APIMessage = fixString + APIMessage + fixString;
	    //if (APIMessage.indexOf("<a href") >= 0){
	    	tMessage.setMovementMethod(LinkMovementMethod.getInstance());
	    	tMessage.setText(Html.fromHtml(APIMessage));
	    /*}
	    else{
	    	tMessage.setText(APIMessage);
	    }*/
	    
	    System.out.println("Message is: " + APIMessage);
	    
	    if (android.os.Build.VERSION.SDK_INT < 9){
	    	ImageButton ib_shareFB = (ImageButton) findViewById(R.id.ib_shareFB);
	    	ib_shareFB.setVisibility(View.INVISIBLE);
	    }
	    
	    tMessage.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View view) {
	            if (tMessage.getSelectionStart() == -1 && tMessage.getSelectionEnd() == -1) { //this is to prevent going to home screen when links are clicked in some deviced 
	            	goToHome(view);
	            }
	        }
	    });
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
	    uiHelper.onResume();
        // Logs 'app activate' App Event.
		AppEventsLogger.activateApp(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
	  super.onPause();
	  uiHelper.onPause();
	  
	  if (Globals.wakelock != null && Globals.wakelock.isHeld()){
		  Globals.wakelock.release();  
	  }

	  // Logs 'app deactivate' App Event.
	  AppEventsLogger.deactivateApp(this);
	}
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    uiHelper.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		// do nothing as we want to prevent back behaviour on this activity
		//super.onBackPressed();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);

	    uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
	        @Override
	        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
	            Log.e("Activity", String.format("Error: %s", error.toString()));
	        }

	        @Override
	        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
	            Log.i("Activity", "Success!");
	        }
	    });
	}
	
	public void goToHome(View view){
		Intent intent = new Intent(this, TimeActivity.class);
	    startActivity(intent);
		
		Intent startMain = new Intent(Intent.ACTION_MAIN);
		startMain.addCategory(Intent.CATEGORY_HOME);
		startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(startMain);
    }
	
	private Session.StatusCallback callback = new Session.StatusCallback() {
		
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			
			if (state.isOpened()) {
				publishFeedDialog();
			}
		}
	};
	
	public void shareFacebook(View view){
		try {
			if (FacebookDialog.canPresentShareDialog(getApplicationContext(), 
			    FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
				// Publish the post using the Share Dialog
				FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
				.setLink("www.ynet.co.il")
				.setCaption("Start your morning with WakeApp")
				.setName("This is how my morning started")
				.setDescription(APIMessage)
				.build();
				uiHelper.trackPendingDialogCall(shareDialog.present());

			} else {
				if (Session.getActiveSession() == null || !Session.getActiveSession().isOpened()) {
			        Session.openActiveSession(this, true, callback);
			    } else {
			        publishFeedDialog();
			    }
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void publishFeedDialog() {
	    Bundle params = new Bundle();
	    params.putString("name", "This is how my morning started");
	    params.putString("caption", "Start your morning with WakeApp");
	    params.putString("description", APIMessage);
	    params.putString("link", "www.ynet.co.il");

	    WebDialog feedDialog = (
	        new WebDialog.FeedDialogBuilder(MessageActivity.this,
	            Session.getActiveSession(),
	            params))
	        .setOnCompleteListener(new OnCompleteListener() {

	            @Override
	            public void onComplete(Bundle values,
	                FacebookException error) {
	                if (error == null) {
	                    // When the story is posted, echo the success
	                    // and the post Id.
	                    final String postId = values.getString("post_id");
	                    if (postId != null) {
	                        Toast.makeText(MessageActivity.this,
	                            "Posted story, id: "+postId,
	                            Toast.LENGTH_SHORT).show();
	                    } else {
	                        // User clicked the Cancel button
	                        Toast.makeText(MessageActivity.this.getApplicationContext(), 
	                            "Publish cancelled", 
	                            Toast.LENGTH_SHORT).show();
	                    }
	                } else if (error instanceof FacebookOperationCanceledException) {
	                    // User clicked the "x" button
	                    Toast.makeText(MessageActivity.this.getApplicationContext(), 
	                        "Publish cancelled", 
	                        Toast.LENGTH_SHORT).show();
	                } else {
	                    // Generic, ex: network error
	                    Toast.makeText(MessageActivity.this.getApplicationContext(), 
	                        "Error posting story", 
	                        Toast.LENGTH_SHORT).show();
	                }
	            }

	        })
	        .build();
	    feedDialog.show();
	}
}

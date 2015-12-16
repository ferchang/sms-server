package com.sms;

//----------------
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
//----------------

import android.telephony.SmsManager;

import android.net.Uri;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;

import android.content.ContentValues;

import java.io.InputStream;
import java.io.IOException; 

import android.util.Log;
import android.content.Intent;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.Headers;

public class main extends Activity
{
	
	public static final int EDIT_ID = Menu.FIRST;
	
	private ClipboardManager clipboard;
	
	private enum Actions { DIRECT, DIRECT8SAVE, BUILTIN, COPY }
	
	private AsyncHttpServer server = new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		Log.d("sms_server", "-------------");
		Log.d("sms_server", "create");		
    }
	
	@Override
    public void onStart() {
        super.onStart();
		Log.d("sms_server", "start");
    }
	
	@Override
    public void onResume() {
        super.onResume();
        startServer();
		Log.d("sms_server", "resume");
    }
	
	@Override
   protected void onPause() {
      super.onPause();
      Log.d("sms_server", "pause");
   }
   
   @Override
   protected void onStop() {
      super.onStop();
      Log.d("sms_server", "stop");
   }

	 @Override
   public void onDestroy() {
	   server.stop();
	   mAsyncServer.stop();
      super.onDestroy();
      Log.d("sms_server", "destroy");
   }
   
   @Override
   protected void onRestart() {
      super.onRestart();
      Log.d("sms_server", "restart");
   }
   
	 private void startServer() {
		 
		 //---------------------------------------------
		 
        server.post("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

				AsyncHttpRequestBody rb=request.getBody();
				Multimap vars=(Multimap) rb.get();
				
				String host=request.getHeaders().get("host");
				
				Actions action=Actions.valueOf(vars.getString("action").toUpperCase());
			
				switch(action) {
					case DIRECT:
					case DIRECT8SAVE:
						Log.d("sms_server", "enum direct/direct8save");
						//if(true) return;
						SmsManager smsManager = SmsManager.getDefault();
						smsManager.sendTextMessage(vars.getString("number"), null, vars.getString("message"), null, null);
						if(action==Actions.DIRECT8SAVE) {
							ContentValues values = new ContentValues();
							values.put("address", vars.getString("number"));
							values.put("body", vars.getString("message"));
							getContentResolver().insert(Uri.parse("content://sms/sent"), values);
						}
					break;
					case BUILTIN:
						Log.d("sms_server", "enum builtin");
						Intent sendIntent = new Intent(Intent.ACTION_VIEW);
						sendIntent.putExtra("sms_body", vars.getString("message")); 
						sendIntent.setType("vnd.android-dir/mms-sms");
						sendIntent.putExtra("address"  , new String (vars.getString("number")));
						startActivity(sendIntent);
					break;
					case COPY:
						Log.d("sms_server", "enum copy");
						final String fmessage=vars.getString("message");
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("message", fmessage);
								clipboard.setPrimaryClip(clip);
							}
						});
					break;
				}
			
				response.send(getRawResourceStr(R.raw.ok).replace("%%host%%", host));
            }
        });
		
		//---------------------------------------------

		server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d("sms_server", "get");
				response.send(getRawResourceStr(R.raw.iface));
            }
        });
		
		//---------------------------------------------
		
        server.listen(mAsyncServer, 8888);
    }
	
	private String getRawResourceStr(int rid) {
			String s="";
		try {
				InputStream is=getResources().openRawResource(rid);
				byte[] reader = new byte[is.available()];
				while(is.read(reader) != -1) {}
				s=new String(reader);
			} catch (IOException e) {}
			return s;
	}
	
	//------------------------------
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, EDIT_ID, Menu.NONE, "Options").setAlphabeticShortcut('o');
		return(super.onCreateOptionsMenu(menu));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case EDIT_ID:
				startActivity(new Intent(this, prefs.class));
				return(true);
		}
		return(super.onOptionsItemSelected(item));
	}
	
	//------------------------------
	
}

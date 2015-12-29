package com.sms;

import java.util.ArrayList;

//----------------
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
//----------------

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.widget.TextView;

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

import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.Headers;

public class Main extends Activity {
	
	private int port;
	
	private final int DEFAULT_PORT=8888;
	
	private static final int MENU_OPTIONS = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST+1;

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
		Log.d("sms_server", "resume");
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		String tmp=prefs.getString("port", "");
		if(tmp.equals("")) port=DEFAULT_PORT;
		else port=Integer.parseInt(tmp);
		Log.d("sms_server", "port: "+Integer.toString(port));
        startServer();
		TextView portLbl = (TextView) findViewById(R.id.port);
		portLbl.setText("Port: "+port);
		
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
						//SmsManager smsManager = SmsManager.getDefault();
						//smsManager.sendTextMessage(vars.getString("number"), null, vars.getString("message"), null, null);
						SmsManager sms = SmsManager.getDefault();
						ArrayList<String> parts = sms.divideMessage(vars.getString("message"));
						
						Log.d("sms_server", "==========================");
						for (int i = 0; i < parts.size(); i++) {
							Log.d("sms_server", i+1+": "+parts.get(i).length()+"\n");
						}
						Log.d("sms_server", "==========================");
						
						Log.d("sms_server", "sendMultipartTextMessage>");
						sms.sendMultipartTextMessage(vars.getString("number"), null, parts, null, null);
						Log.d("sms_server", "<sendMultipartTextMessage");
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
								ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
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
		
		test ttt=new test(server, this);

		//---------------------------------------------
		
		server.stop();
		mAsyncServer.stop();
		
		server.setErrorCallback(ttt);
		
		AsyncServerSocket tmp=server.listen(mAsyncServer, port);
		if(tmp==null) {
			Log.d("sms_server", "server start failed");
			findViewById(R.id.status_view).setBackgroundResource(R.drawable.server_off_led);
		}
		else {
			Log.d("sms_server", "server started");
			findViewById(R.id.status_view).setBackgroundResource(R.drawable.server_on_led);
		}
        
    }
	
	protected String getRawResourceStr(int rid) {
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
		menu.add(Menu.NONE, MENU_OPTIONS, Menu.NONE, "Options");
		menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, "About");
		return(super.onCreateOptionsMenu(menu));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_OPTIONS:
				startActivity(new Intent(this, Prefs.class));
			break;
			case MENU_ABOUT:
				startActivity(new Intent(this, About.class));
			break;
		}
		return(super.onOptionsItemSelected(item));
	}
	
	//------------------------------
	
}

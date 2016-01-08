package com.sms;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.net.Uri;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.ContentValues;
import java.util.ArrayList;
import android.telephony.SmsManager;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import android.util.Log;
import android.app.Activity;
import android.content.Context;

import com.koushikdutta.async.callback.CompletedCallback;

class HttpResponder implements HttpServerRequestCallback, CompletedCallback {
	
	private int authFlag;
	
	private Main actvt;
	
	private enum Actions { DIRECT, DIRECT8SAVE, BUILTIN, COPY }
	
	HttpResponder(AsyncHttpServer server, final Main actvt) {
		this.actvt=actvt;
		server.get("/", this);
		server.get("/jquery.js", this);
		server.get("/jscookie.js", this);
		server.post("/action", this);
		server.setErrorCallback(this);
	}
	
	private boolean auth() {
		
		authFlag=-1;
		
		final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						Log.d("sms_server", "yes");
						authFlag=1;
					break;
					case DialogInterface.BUTTON_NEGATIVE:
						Log.d("sms_server", "no");
						authFlag=0;
						break;
				}
			}
		};		
		
		actvt.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(actvt);
				builder.setMessage("Accept connection?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
			}
		});
		
		try { while(authFlag==-1) Thread.sleep(300); }
		catch(InterruptedException e) {}
		
		return true;
	}
	
	@Override
	public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
		if(!auth()) {
			response.send("auth failed");
			return;
		}
		String path=request.getPath();
		Log.d("sms_server", path);
		if(path.equals("/"))
			response.send(actvt.getRawResourceStr(R.raw.iface));
		else if(path.equals("/jquery.js"))
				response.send(actvt.getRawResourceStr(R.raw.jquery));
		else if(path.equals("/jscookie.js"))
				response.send(actvt.getRawResourceStr(R.raw.jscookie));
		else if(path.equals("/action")) {
		//================================================
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
							actvt.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
						}
					break;
					case BUILTIN:
						Log.d("sms_server", "enum builtin");
						Intent sendIntent = new Intent(Intent.ACTION_VIEW);
						sendIntent.putExtra("sms_body", vars.getString("message")); 
						sendIntent.setType("vnd.android-dir/mms-sms");
						sendIntent.putExtra("address"  , new String (vars.getString("number")));
						actvt.startActivity(sendIntent);
					break;
					case COPY:
						Log.d("sms_server", "enum copy");
						final String fmessage=vars.getString("message");
						actvt.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								ClipboardManager clipboard = (ClipboardManager) actvt.getSystemService(Context.CLIPBOARD_SERVICE);
								ClipData clip = ClipData.newPlainText("message", fmessage);
								clipboard.setPrimaryClip(clip);
							}
						});
					break;
				}
			
				response.send(actvt.getRawResourceStr(R.raw.ok).replace("%%host%%", host));
		//================================================
		}
		else {
			response.send("unknown path: "+path);
			Log.d("sms_server", "unknown path: "+path);
		}
	}
	
	public void onCompleted(Exception ex) {
		Log.d("sms_server", ex.getMessage());
	}
	
}

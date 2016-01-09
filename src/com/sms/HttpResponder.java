package com.sms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.UUID;

import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.koushikdutta.async.http.Headers;
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
	
	//-----------------------------------------------------
	
	HttpResponder(AsyncHttpServer server, final Main actvt) {
		this.actvt=actvt;
		server.get("/", this);
		server.post("/", this);
		server.get("/jquery.js", this);
		server.get("/jscookie.js", this);
		server.post("/action", this);
		server.setErrorCallback(this);
	}
	
	//-----------------------------------------------------
	
	String resourceStrReplace(int rid, String needle, String haystack) {
		return actvt.getRawResourceStr(rid).replace(needle, haystack);
	}
	
	//-----------------------------------------------------
	
	private boolean auth(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
		
		final String path=request.getPath();
		final String reqMethod=request.getMethod();
		final SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(actvt);
		final String auth_token=prefs.getString("auth_token", "");
		final String password0=prefs.getString("password", "");
		final boolean manual_auth=prefs.getBoolean("manual_auth", false);
		boolean password_auth=prefs.getBoolean("password_auth", false);
		
		if(!manual_auth && !password_auth) return true;
		
		if(!manual_auth && password_auth && password0.equals("")) {
			response.send("Error: No password is set!");
			return false;
		}
		
		if(manual_auth && password_auth && password0.equals("")) password_auth=false;
		
		Headers headers=request.getHeaders();
		String cookies=headers.get("cookie");
		Log.d("sms_server", "cookies: "+cookies);
		if(cookies!=null && !auth_token.equals("") && cookies.indexOf(auth_token)!=-1) {
			Log.d("sms_server", "auth ok");
			return true;
		}
		else {
			Log.d("sms_server", "auth not ok");
			if(path.equals("/action")) {
				response.send("Access denied!");
				return false;
			}
		}
		
		if(password_auth && reqMethod.equals("GET")) {
			String msg="";
			if(manual_auth) msg="Send an empty password for manual confirmation on device<br>";
			response.send(resourceStrReplace(R.raw.auth, "%%msg%%", msg));
			return false;
		}
		
		if(password_auth) {
			AsyncHttpRequestBody rb=request.getBody();
			Multimap vars=(Multimap) rb.get();
			String password1=vars.getString("password");
			Log.d("sms_server", password0+"/"+password1);
			if(password0.equals(password1)) {
				String tok=UUID.randomUUID().toString();
				response.send(resourceStrReplace(R.raw.iface, "//%%auth%%", "Cookies.set('sms_server_auth', '"+tok+"');"));
				Editor editor=prefs.edit();
				editor.putString("auth_token", tok);
				editor.commit();
				return false;
			}
			else if(!manual_auth || !password1.equals("")) {
					String msg="";
					if(manual_auth) msg="Send an empty password for manual confirmation on device<br>";
					response.send(resourceStrReplace(R.raw.auth, "%%msg%%", msg));
					return false;
			}
		}

		authFlag=-1;
		
		final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
					case DialogInterface.BUTTON_POSITIVE:
						Log.d("sms_server", "yes");
						authFlag=1;
						String out;
						if(path.equals("/")) out=actvt.getRawResourceStr(R.raw.iface);
						else out=resourceStrReplace(R.raw.ok, "%%host%%", request.getHeaders().get("host"));
						String tok=UUID.randomUUID().toString();
						out=out.replace("//%%auth%%", "Cookies.set('sms_server_auth', '"+tok+"');");
						response.send(out);
						Editor editor=prefs.edit();
						editor.putString("auth_token", tok);
						editor.commit();
					break;
					case DialogInterface.BUTTON_NEGATIVE:
						Log.d("sms_server", "no");
						authFlag=0;
						response.send("Access denied!");
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
		
		return false;
		
	}
	
	//-----------------------------------------------------
	
	@Override
	public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
		
		String path=request.getPath();
		Log.d("sms_server", path);
		
		if(path.equals("/") || path.equals("/action")) if(!auth(request, response)) return;
	
		if(path.equals("/"))
			response.send(actvt.getRawResourceStr(R.raw.iface));
		else if(path.equals("/jquery.js"))
			response.send(actvt.getRawResourceStr(R.raw.jquery));
		else if(path.equals("/jscookie.js"))
			response.send(actvt.getRawResourceStr(R.raw.jscookie));
		else if(path.equals("/action")) {
		//===================
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
		//====================
		}
		else {
			response.send("unknown path: "+path);
			Log.d("sms_server", "unknown path: "+path);
		}
	}
	
	//-----------------------------------------------------
	
	public void onCompleted(Exception ex) {
		Log.d("sms_server", ex.getMessage());
	}
	
}

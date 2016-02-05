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
	
	Main actvt;
	
	private enum Actions { DIRECT, DIRECT8SAVE, BUILTIN, COPY }
	
	//-----------------------------------------------------
	
	HttpResponder(AsyncHttpServer server, final Main actvt) {
		this.actvt=actvt;
		server.get("/logout", this);
		server.get("/", this);
		server.post("/", this);
		server.get("/jquery.js", this);
		server.get("/jscookie.js", this);
		server.post("/action", this);
		server.setErrorCallback(this);
	}
	
	//-----------------------------------------------------
	
	String rawResourceStrReplace(int rid, String needle, String haystack) {
		return rawResourceStr(rid).replace(needle, haystack);
	}
	
	//-----------------------------------------------------
	
	String rawResourceStr(int rid) {
		return actvt.getRawResourceStr(rid);
	}
	
	//-----------------------------------------------------
	
	void showErrorPage(AsyncHttpServerRequest request, AsyncHttpServerResponse response, String msg) {
		String out=rawResourceStr(R.raw.error);
		out=out.replace("%%host%%", request.getHeaders().get("host"));
		out=out.replace("%%msg%%", "Error: "+msg+"!");
		response.send(out);
	}
	
	//-----------------------------------------------------
	
	String createNewAuthToken() {
		final SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(actvt);
		String tok=UUID.randomUUID().toString();
		Editor editor=prefs.edit();
		editor.putString("auth_token", tok);
		editor.commit();
		return tok;
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
			showErrorPage(request, response, "No password is set");
			return false;
		}
		
		if(manual_auth && password_auth && password0.equals("")) password_auth=false;
		
		String cookies=getRequestCookies(request);
		Log.d("sms_server", "cookies: "+cookies);
		if(!auth_token.equals("") && cookies.indexOf(auth_token)!=-1) {
			Log.d("sms_server", "auth ok");
			return true;
		}
		else {
			Log.d("sms_server", "auth not ok");
			if(path.equals("/action")) {
				showErrorPage(request, response, "Access denied");
				return false;
			}
		}
		
		if(password_auth && reqMethod.equals("GET")) {
			String msg="";
			if(manual_auth) msg="Send an empty password for manual confirmation on device<br>";
			response.send(addCsrfToken(request, rawResourceStrReplace(R.raw.auth, "%%msg%%", msg)));
			return false;
		}
		
		if(password_auth) {
			String password1=getPostVar(request, "password");
			Log.d("sms_server", password0+"/"+password1);
			if(password0.equals(password1)) {
				Log.d("sms_server", "password ok");
				response.send(addCsrfToken(request, addAuthCookie(rawResourceStr(R.raw.iface))));
				return false;
			}
			else if(!manual_auth || !password1.equals("")) {
					String msg="";
					if(manual_auth) msg="Send an empty password for manual confirmation on device<br>";
					response.send(addCsrfToken(request, rawResourceStrReplace(R.raw.auth, "%%msg%%", msg)));
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
						response.send(addCsrfToken(request, addAuthCookie(rawResourceStr(R.raw.iface))));
					break;
					case DialogInterface.BUTTON_NEGATIVE:
						Log.d("sms_server", "no");
						authFlag=0;
						showErrorPage(request, response, "Access denied");
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
	
	String getRequestCookies(AsyncHttpServerRequest request) {
		String cookies=request.getHeaders().get("cookie");
		if(cookies!=null) return cookies;
		return "";
	}
	
	//-----------------------------------------------------
	
	String addCsrfToken(AsyncHttpServerRequest request, String html) {
		String cookies=getRequestCookies(request);
		int pos=cookies.indexOf("sms_server_csrf"); 
		String tok;
		if(pos!=-1) tok=cookies.substring(pos+16, pos+16+36);
		else {
			tok=UUID.randomUUID().toString();
			html=html.replace("//%%csrf_cookie%%", "Cookies.set('sms_server_csrf', '"+tok+"');");
		}
		html=html.replace("%%csrf_token%%", tok);
		return html;
	}
	
	//-----------------------------------------------------
	
	String addAuthCookie(String html) {
		html=html.replace("//%%auth_cookie%%", "Cookies.set('sms_server_auth', '"+createNewAuthToken()+"');");
		Log.d("sms_server", "addAuthCookie");
		return html;
	}
	
	//-----------------------------------------------------
	
	String getPostVar(AsyncHttpServerRequest request, String name) {
		AsyncHttpRequestBody rb=request.getBody();
		Multimap vars=(Multimap) rb.get();
		return vars.getString(name);
	}
	
	//-----------------------------------------------------
	
	boolean checkCsrfToken(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
		String cookies=getRequestCookies(request);
		int pos=cookies.indexOf("sms_server_csrf"); 
		if(pos==-1) {
			showErrorPage(request, response, "sms_server_csrf cookie not set");
			return false;
		}
		String tok=cookies.substring(pos+16, pos+16+36);
		if(tok.equals(getPostVar(request, "csrf_token"))) return true;
		showErrorPage(request, response, "CSRF token mismatch");
		return false;
	}
	
	//-----------------------------------------------------
	
	@Override
	public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
		
		String path=request.getPath();
		Log.d("sms_server", path);
		final String reqMethod=request.getMethod();
		String host=request.getHeaders().get("host");
				
		if(reqMethod.equals("POST")) if(!checkCsrfToken(request, response)) return;
		
		if(path.equals("/") || path.equals("/action")) if(!auth(request, response)) return;
	
		if(path.equals("/"))
			response.send(addCsrfToken(request, rawResourceStr(R.raw.iface)));
		else if(path.equals("/logout"))
			response.send(rawResourceStrReplace(R.raw.logout, "%%host%%", host));
		else if(path.equals("/jquery.js"))
			response.send(rawResourceStr(R.raw.jquery));
		else if(path.equals("/jscookie.js"))
			response.send(rawResourceStr(R.raw.jscookie));
		else if(path.equals("/action")) {
		//===============================================================================
				Actions action=Actions.valueOf(getPostVar(request, "action").toUpperCase());
			
				switch(action) {
					case DIRECT:
					case DIRECT8SAVE:
						Log.d("sms_server", "enum direct/direct8save");
						SmsManager sms = SmsManager.getDefault();
						ArrayList<String> parts = sms.divideMessage(getPostVar(request,"message"));
						
						Log.d("sms_server", "==========================");
						for (int i = 0; i < parts.size(); i++) {
							Log.d("sms_server", i+1+": "+parts.get(i).length()+"\n");
						}
						Log.d("sms_server", "==========================");
						
						Log.d("sms_server", "sendMultipartTextMessage>");
						sms.sendMultipartTextMessage(getPostVar(request,"number"), null, parts, null, null);
						Log.d("sms_server", "<sendMultipartTextMessage");
						if(action==Actions.DIRECT8SAVE) {
							ContentValues values = new ContentValues();
							values.put("address", getPostVar(request,"number"));
							values.put("body", getPostVar(request,"message"));
							actvt.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
						}
					break;
					case BUILTIN:
						Log.d("sms_server", "enum builtin");
						Intent sendIntent = new Intent(Intent.ACTION_VIEW);
						sendIntent.putExtra("sms_body", getPostVar(request,"message")); 
						sendIntent.setType("vnd.android-dir/mms-sms");
						sendIntent.putExtra("address"  , new String (getPostVar(request,"number")));
						actvt.startActivity(sendIntent);
					break;
					case COPY:
						Log.d("sms_server", "enum copy");
						final String fmessage=getPostVar(request,"message");
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
			
				response.send(rawResourceStrReplace(R.raw.ok, "%%host%%", host));
		//===============================================================================
		}
		else {
			showErrorPage(request, response, "Unknown path: "+path);
			Log.d("sms_server", "unknown path: "+path);
		}
	}
	
	//-----------------------------------------------------
	
	public void onCompleted(Exception ex) {
		Log.d("sms_server", ex.getMessage());
	}
	
}

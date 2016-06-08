package hmz2627_at_gmail_dot_com.sms_server;

import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.UUID;

import android.content.SharedPreferences.Editor;

import android.app.DialogFragment;

import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.content.IntentFilter;

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

import com.koushikdutta.async.callback.CompletedCallback;

class HttpResponder implements HttpServerRequestCallback, CompletedCallback {
	
	private int authFlag;
	
	Main actvt;

	String SENT = "SMS_SENT";
	String DELIVERED = "SMS_DELIVERED";
	
	SentIntentReceiver sir;
	DeliverIntentReceiver dir;
	
	private enum Actions { DIRECT, DIRECT8SAVE, BUILTIN, COPY }
	
	//-----------------------------------------------------
	
	HttpResponder(AsyncHttpServer server, final Main actvt) {
		this.actvt=actvt;
		server.get("/logout", this);
		server.get("/clipboard", this);
		server.get("/", this);
		server.post("/", this);
		server.get("/all.js", this);
		server.post("/action", this);
		server.get("/report", this);
		server.setErrorCallback(this);
		
		this.sir=new SentIntentReceiver(actvt);
		this.dir=new DeliverIntentReceiver(actvt);
		actvt.registerReceiver(sir, new IntentFilter(SENT));
		actvt.registerReceiver(dir, new IntentFilter(DELIVERED));
		
		PendingIntentsInfo.httpResponder=this;
		PendingIntentsInfo.requestCode=Prefs.p.getInt("lastRequestCode", -1);
		
	}

	//-----------------------------------------------------
	
	String rawResourceStr(int rid) {
		return actvt.getRawResourceStr(rid);
	}
	
	//-----------------------------------------------------
	
	void showErrorPage(AsyncHttpServerRequest request, AsyncHttpServerResponse response, String msg) {
		String out=getPreprocessedHtml(R.raw.error);
		out=out.replace("%%host%%", request.getHeaders().get("host"));
		out=out.replace("%%msg%%", getString(R.string.error)+": "+msg+"!");
		response.send(out);
	}
	
	//-----------------------------------------------------
	
	String createNewAuthToken() {
		String tok=UUID.randomUUID().toString();
		Editor editor=Prefs.p.edit();
		editor.putString("auth_token", tok);
		editor.commit();
		return tok;
	}
	
	//-----------------------------------------------------
	
	String getString(int rid) {
		return actvt.getResources().getString(rid);
	}
	
	//-----------------------------------------------------
	
	void showAuthPage(AsyncHttpServerRequest request, AsyncHttpServerResponse response, boolean manual_auth, String password1) {
		String out=addCsrfToken(request, getPreprocessedHtml(R.raw.auth));
		if(!password1.equals("")) out=out.replace("<!--%%error_msg%%-->", "<h4 style='color: red; background: yellow; display: inline; padding: 5px'>"+getString(R.string.password_wrong)+"</h4><br><br>");
		if(manual_auth) out=out.replace("<!--%%msg%%-->", getString(R.string.manual_possible)+"<br>");
		response.send(out);
	}
	
	//-----------------------------------------------------
	
	private boolean auth(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
		
		final String path=request.getPath();
		final String reqMethod=request.getMethod();
		final String auth_token=Prefs.p.getString("auth_token", "");
		final String password0=Prefs.p.getString("password", "");
		final boolean manual_auth=Prefs.p.getBoolean("manual_auth", false);
		boolean password_auth=Prefs.p.getBoolean("password_auth", false);
		
		if(!manual_auth && !password_auth) return true;
		
		if(!manual_auth && password_auth && password0.equals("")) {
			showErrorPage(request, response, getString(R.string.no_password));
			return false;
		}
		
		if(manual_auth && password_auth && password0.equals("")) password_auth=false;
		
		String cookies=getRequestCookies(request);
		Logger.d("cookies: "+cookies);
		if(!auth_token.equals("") && cookies.indexOf(auth_token)!=-1) {
			Logger.d("auth ok");
			return true;
		}
		else {
			Logger.d("auth not ok");
			if(path.equals("/action")) {
				showErrorPage(request, response, "Access denied");
				return false;
			}
		}
		
		if(path.equals("/clipboard")) {
			response.send("Access denied!");
			return false;
		}
		
		if(password_auth && reqMethod.equals("GET")) {
			showAuthPage(request, response, manual_auth, "");
			return false;
		}
		
		if(password_auth) {
			String password1=getPostVar(request, "password");
			Logger.d(password0+"/"+password1);
			if(password0.equals(password1)) {
				Logger.d("password ok");
				response.send(addCsrfToken(request, addAuthCookie(getPreprocessedHtml(R.raw.iface))));
				return false;
			}
			else {
				if(!manual_auth || !password1.equals("")) {
					showAuthPage(request, response, manual_auth, password1);
					return false;
				}
			}
		}
		
		authFlag=-1;
		
		final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which) {
					case DialogInterface.BUTTON_POSITIVE:
						Logger.d("yes");
						authFlag=1;
						response.send(addCsrfToken(request, addAuthCookie(getPreprocessedHtml(R.raw.iface))));
					break;
					case DialogInterface.BUTTON_NEGATIVE:
						Logger.d("no");
						authFlag=0;
						showErrorPage(request, response, getString(R.string.access_denied));
					break;
					default:
						Logger.d("default");
					break;
				}
			}
		};		
		
		//--------------------------------------
		final DialogInterface.OnCancelListener dialogCancelListener = new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				Logger.d("dialog cancelled");
				dialogClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
			}
		};
		//--------------------------------------
		
		Current.currentActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(Current.currentActivity);
				AlertDialog dialog=builder.setMessage(getString(R.string.accept_conn)).setPositiveButton(getString(R.string.yes), dialogClickListener).setNegativeButton(getString(R.string.no), dialogClickListener).create();
				dialog.setOnCancelListener(dialogCancelListener);
				dialog.setCancelable(false);
				dialog.show();
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
		Logger.d("addAuthCookie");
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
	
	String getAllJavascript(AsyncHttpServerRequest request) {
		
		String sp="\n//######################################################\n\n";
		
		String out=sp+rawResourceStr(R.raw.jquery)+sp+rawResourceStr(R.raw.jscookie)+sp+rawResourceStr(R.raw.tr)+sp+rawResourceStr(R.raw.common_strings)+sp;
		
		int rid=actvt.getResources().getIdentifier(request.getQuery().getString("file")+"_strings", "raw", actvt.getPackageName());
		
		if(rid!=0) out=out+rawResourceStr(rid)+sp;
		
		return out;
		
	}
	
	//-----------------------------------------------------
		
	String getPreprocessedHtml(int rid) {
		String out=rawResourceStr(rid);
		out=out.replaceAll("//([A-Za-z0-9_]+?)//", "<script>tr('$1')</script>");
		out=out.replaceFirst("(?i)<html>([\r\n])*<head>", "<html>$1<head>\n<script src='all.js?file="+actvt.getResources().getResourceEntryName(rid)+"'></script>");
		return out.replace("%%js_off_msg%%", "<noscript><h2 style='color: red; background: #000; padding: 5px; text-align: center'>"+getString(R.string.js_off_msg)+"</h2></noscript>");
	}
	
	//-----------------------------------------------------
	
	@Override
	public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
		
		String path=request.getPath();
		Logger.d(path);
		final String reqMethod=request.getMethod();
		String host=request.getHeaders().get("host");
		
		//if(path.equals("/")) try { Thread.sleep(10000); } catch(InterruptedException e) {}
		
		if(reqMethod.equals("POST")) if(!checkCsrfToken(request, response)) return;
		
		if(path.equals("/") || path.equals("/action") || path.equals("/clipboard"))
			if(!auth(request, response)) return;
	
		if(path.equals("/"))
			response.send(addCsrfToken(request, getPreprocessedHtml(R.raw.iface)));
		else if(path.equals("/clipboard")) {
			actvt.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					ClipboardManager clipboard = (ClipboardManager) actvt.getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = clipboard.getPrimaryClip();
					if(clip!=null) {
						ClipData.Item item = clip.getItemAt(0);
						String text= item.getText().toString();
						Logger.d("clipboard: "+text);
						response.send(text);
					}
					else {
						Logger.d("clipboard empty");
						response.send("");
					}
				}
			});
		}
		else if(path.equals("/logout"))
			response.send(getPreprocessedHtml(R.raw.logout).replace("%%host%%", host));
		else if(path.equals("/all.js"))
			response.send(getAllJavascript(request));
		else if(path.equals("/report")) {
			int req_code=Integer.parseInt(request.getQuery().getString("req_code"));
			response.send(PendingIntentsInfo.getResults(req_code));
		}
		else if(path.equals("/action")) {
		//===============================================================================
				Actions action=Actions.valueOf(getPostVar(request, "action").toUpperCase());
				
				int reqCodeFlag=-1;
			
				switch(action) {
					case DIRECT:
					case DIRECT8SAVE:
						Logger.d("enum direct/direct8save");
						
						//-----------------------------------
						
						int requestCode=PendingIntentsInfo.requestCode;
	
						requestCode++;
						reqCodeFlag=requestCode;
						Intent ii=new Intent(SENT);
						ii.putExtra("requestCode", requestCode);
						PendingIntent sentPI = PendingIntent.getBroadcast(actvt, requestCode, ii, PendingIntent.FLAG_CANCEL_CURRENT|PendingIntent.FLAG_ONE_SHOT);
						
						requestCode++;
						ii=new Intent(DELIVERED);
						ii.putExtra("requestCode", requestCode);
						PendingIntent deliveredPI = PendingIntent.getBroadcast(actvt, requestCode, ii, PendingIntent.FLAG_CANCEL_CURRENT|PendingIntent.FLAG_ONE_SHOT);
						
						PendingIntentsInfo.requestCode=requestCode;
						Editor editor = Prefs.p.edit();
						editor.putInt("lastRequestCode", requestCode);
						editor.apply();

						ArrayList<PendingIntent> spi=new ArrayList<PendingIntent>();
						spi.add(sentPI);
						ArrayList<PendingIntent> dpi=new ArrayList<PendingIntent>();
						dpi.add(deliveredPI);
						//-----------------------------------
						
						SmsManager sms = SmsManager.getDefault();
						ArrayList<String> parts = sms.divideMessage(getPostVar(request,"message"));
						
						Logger.d("==========================");
						for (int i = 0; i < parts.size(); i++) {
							Logger.d(i+1+": "+parts.get(i).length()+"\n");
						}
						Logger.d("==========================");
						
						Logger.d("sending sms >");
						sms.sendMultipartTextMessage(getPostVar(request,"number"), null, parts, spi, dpi);
						Logger.d("sending sms <");
						
						if(action==Actions.DIRECT8SAVE) {
							ContentValues values = new ContentValues();
							values.put("address", getPostVar(request,"number"));
							values.put("body", getPostVar(request,"message"));
							actvt.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
						}
					break;
					case BUILTIN:
						Logger.d("enum builtin");
						Intent sendIntent = new Intent(Intent.ACTION_VIEW);
						sendIntent.putExtra("sms_body", getPostVar(request,"message")); 
						sendIntent.setType("vnd.android-dir/mms-sms");
						sendIntent.putExtra("address"  , new String (getPostVar(request,"number")));
						actvt.startActivity(sendIntent);
					break;
					case COPY:
						Logger.d("enum copy");
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
				
				response.send(getPreprocessedHtml(R.raw.ok).replace("%%host%%", host).replace("%%message_processed%%", getString(R.string.message_processed)).replace("//%%req_code%%", "req_code="+reqCodeFlag));
				
		//===============================================================================
		}
		else {
			showErrorPage(request, response, "Unknown path: "+path);
			Logger.d("unknown path: "+path);
		}
	}
	
	//-----------------------------------------------------
	
	public void onCompleted(Exception ex) {
		Logger.d(ex.getMessage());
	}
	
}

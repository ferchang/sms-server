package com.sms;

import android.telephony.SmsManager;

import android.net.Uri;

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
	
	public enum actions { direct, direct8save, builtin, copy }
	
	private AsyncHttpServer server = new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();
	
	Button btn;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
				
    }
	
		
	//--------------------------
	
	@Override
    public void onResume() {
        super.onResume();
        startServer();
		Log.d("sms_server", "#############################");
    }
	
	 private void startServer() {
		 
		 //---------------------------------------------
		 
        server.post("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

				AsyncHttpRequestBody rb=request.getBody();
				Multimap vars=(Multimap) rb.get();
				
				String host=request.getHeaders().get("host");
				
				actions action=actions.valueOf(vars.getString("action"));
			
				switch(action) {
					case direct:
					case direct8save:
						Log.d("sms_server", "enum direct/direct8save");
						if(true) return;
						SmsManager smsManager = SmsManager.getDefault();
						smsManager.sendTextMessage(vars.getString("number"), null, vars.getString("message"), null, null);
						if(action==actions.direct8save) {
							ContentValues values = new ContentValues();
							values.put("address", vars.getString("number"));
							values.put("body", vars.getString("message"));
							getContentResolver().insert(Uri.parse("content://sms/sent"), values);
						}
					break;
					case builtin:
						Log.d("sms_server", "enum builtin");
						Intent sendIntent = new Intent(Intent.ACTION_VIEW);
						sendIntent.putExtra("sms_body", vars.getString("message")); 
						sendIntent.setType("vnd.android-dir/mms-sms");
						sendIntent.putExtra("address"  , new String (vars.getString("number")));
						startActivity(sendIntent);
					break;
					case copy:
						Log.d("sms_server", "enum copy");
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
	
}

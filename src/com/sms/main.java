package com.sms;

import android.telephony.SmsManager;

import java.io.InputStream;
import java.io.IOException; 

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

public class main extends Activity
{
	
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
    }
	
	 private void startServer() {
		 
		 //---------------------------------------------
		 
        server.post("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

				AsyncHttpRequestBody rb=request.getBody();
				Multimap vars=(Multimap) rb.get();
			
				if(!"yes".equals(vars.getString("via_builtin_app"))) {
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(vars.getString("number"), null, vars.getString("message"), null, null);
				}
				else {
					Intent sendIntent = new Intent(Intent.ACTION_VIEW);
					sendIntent.putExtra("sms_body", vars.getString("message")); 
					sendIntent.setType("vnd.android-dir/mms-sms");
					sendIntent.putExtra("address"  , new String (vars.getString("number")));
					startActivity(sendIntent);
				}
				
				response.send(getRawResourceStr(R.raw.ok));
            }
        });
		
		//---------------------------------------------

		server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
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

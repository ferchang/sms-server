package com.sms;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import android.util.Log;
import android.app.Activity;
import android.content.Context;

import com.koushikdutta.async.callback.CompletedCallback;

class test implements CompletedCallback {
	
	test(AsyncHttpServer server, final Main actvt) {
		
		server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d("sms_server", "get");
				response.send(actvt.getRawResourceStr(R.raw.iface));
            }
        });
		
		//----------------------------------
		
		server.get("/jquery.js", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d("sms_server", "get jquery.js");
				response.send(actvt.getRawResourceStr(R.raw.jquery));
            }
        });
		
		//----------------------------------
		
		
		server.get("/jscookie.js", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
				Log.d("sms_server", "get jscookie.js");
				response.send(actvt.getRawResourceStr(R.raw.jscookie));
            }
        });
		
	}
	
	public void onCompleted(Exception ex) {
		Log.d("sms_server", ex.getMessage());
	}
	
}
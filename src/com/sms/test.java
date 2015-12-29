package com.sms;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import android.util.Log;
import android.app.Activity;
import android.content.Context;

import com.koushikdutta.async.callback.CompletedCallback;

class test implements HttpServerRequestCallback, CompletedCallback {
	
	private Main actvt;
	
	test(AsyncHttpServer server, final Main actvt) {
		this.actvt=actvt;
		server.get("/", this);
		server.get("/jquery.js", this);
		server.get("/jscookie.js", this);
	}
	
	@Override
	public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
		String path=request.getPath();
		Log.d("sms_server", path);
		if(path.equals("/"))
			response.send(actvt.getRawResourceStr(R.raw.iface));
		else if(path.equals("/jquery.js"))
				response.send(actvt.getRawResourceStr(R.raw.jquery));
		else if(path.equals("/jscookie.js"))
				response.send(actvt.getRawResourceStr(R.raw.jscookie));
		else {
			response.send("unknown path: "+path);
			Log.d("sms_server", "unknown path: "+path);
		}
	}
	
	public void onCompleted(Exception ex) {
		Log.d("sms_server", ex.getMessage());
	}
	
}

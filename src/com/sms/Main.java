package com.sms;
//
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.widget.TextView;

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

public class Main extends Activity {
	
	private int port;
	
	private final int DEFAULT_PORT=8888;
	
	private static final int MENU_OPTIONS = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST+1;
	
	private AsyncHttpServer server = new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		Log.d("sms_server", "---------------------------------");
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
	
		new HttpResponder(server, this);
		
		server.stop();
		mAsyncServer.stop();
		
		AsyncServerSocket tmp=server.listen(mAsyncServer, port);
		if(tmp==null) {
			Log.d("sms_server", "server start failed");
			findViewById(R.id.status_view).setBackgroundResource(R.drawable.server_off_lamp);
		}
		else {
			Log.d("sms_server", "server started");
			findViewById(R.id.status_view).setBackgroundResource(R.drawable.server_on_lamp);
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

}

package hmz2627_at_gmail_dot_com.sms_server;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.util.Locale;
import android.content.res.Configuration;
import android.content.res.Resources;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.content.SharedPreferences.Editor;

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
	
	private String serverFlag="x";
	
	private String lang;
	
	private final int DEFAULT_PORT=8888;
	
	private static final int MENU_OPTIONS = Menu.FIRST;
	private static final int MENU_ABOUT = Menu.FIRST+1;
	
	private AsyncHttpServer server = new AsyncHttpServer();
    private AsyncServer mAsyncServer = new AsyncServer();
	
	public void ipsBtnClick(View view) {
		Intent intent = new Intent(getApplicationContext(), IPs.class);
		startActivity(intent);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		
		PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
		
		Prefs.p=PreferenceManager.getDefaultSharedPreferences(this);
		
		Logger.d("---------------------------------");
		Logger.d("create");
		
		Logger.d("OS Locale: "+Resources.getSystem().getConfiguration().locale);
		
		String lang=Prefs.p.getString("lang", "default");
		Logger.d("lang: "+lang);
		if(!lang.equals("default")) MyLocale.changeLocale(this, lang);
		
		this.lang=lang;
		
        setContentView(R.layout.main);
    }
	
	@Override
    public void onStart() {
        super.onStart();
		Logger.d("start");
    }
	
	//----------------------------------------
	@Override
	public Object onRetainNonConfigurationInstance() {
		Logger.d("onRetainNonConfigurationInstance");
		return new Object();
	}
	//----------------------------------------
	
	@Override
    public void onResume() {
        super.onResume();
		Current.currentActivity=this;
		Logger.d("resume");
		Prefs.p.getString("lang", "x");
		String tmp=Prefs.p.getString("port", "");
		//------------------------
		if(serverFlag.equals(tmp)) return;
		//------------------------
		if(tmp.equals("")) {
			port=DEFAULT_PORT;
			Editor editor = Prefs.p.edit();
			editor.putString("port", Integer.toString(port));
			editor.commit();
		}
		else port=Integer.parseInt(tmp);
		Logger.d("port: "+Integer.toString(port));
		TextView portLbl = (TextView) findViewById(R.id.port);
		portLbl.setText(getResources().getString(R.string.port)+": "+startServer());
		serverFlag=Integer.toString(port);
		
    }
	
	@Override
   protected void onPause() {
      super.onPause();
      Logger.d("pause");
   }
   
   @Override
   protected void onStop() {
      super.onStop();
      Logger.d("stop");
   }

	 @Override
   public void onDestroy() {
	   server.stop();
	   mAsyncServer.stop();
      super.onDestroy();
      Logger.d("destroy");
   }
   
   @Override
   protected void onRestart() {
		super.onRestart();
		Logger.d("restart");
		String lang=Prefs.p.getString("lang", "default");
		if(!lang.equals(this.lang)) {
			Logger.d("calling recreate...");
			MyLocale.changeLocale(this, lang);
			recreate();	  
		}
   }
   
	 private int startServer() {
	
		new HttpResponder(server, this);
		
		server.stop();
		mAsyncServer.stop();
		
		AsyncServerSocket tmp=server.listen(mAsyncServer, port);
		
		if(tmp==null) {
			Logger.d("server start failed");
			findViewById(R.id.status_view).setBackgroundResource(R.drawable.server_off_lamp);
			return -1;
		}
		else {
			Logger.d("server started");
			Logger.d("Local port: "+Integer.toString(tmp.getLocalPort()));
			findViewById(R.id.status_view).setBackgroundResource(R.drawable.server_on_lamp);
			return tmp.getLocalPort();
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
		menu.add(Menu.NONE, MENU_OPTIONS, Menu.NONE, R.string.options);
		menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.about);
		return(super.onCreateOptionsMenu(menu));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_OPTIONS:
				startActivity(new Intent(this, Preferences.class));
			break;
			case MENU_ABOUT:
				startActivity(new Intent(this, About.class));
			break;
		}
		return(super.onOptionsItemSelected(item));
	}

}

package hmz2627_at_gmail_dot_com.sms_server;

import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Logger {
	
	public static void d(String m) {
		if(!Prefs.p.getBoolean("debug", false)) return;
		Log.d("sms_server", m);
	}
	
}

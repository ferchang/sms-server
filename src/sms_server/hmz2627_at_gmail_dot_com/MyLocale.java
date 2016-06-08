package hmz2627_at_gmail_dot_com.sms_server;

import java.util.Locale;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.app.Activity;

public class MyLocale {

	public static void changeLocale(Activity actvt, String cc) {
		Logger.d("changing locale to: "+cc+"...");
		Locale locale;
		if(cc.equals("default")) locale=Resources.getSystem().getConfiguration().locale;
		else locale = new Locale(cc);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		actvt.getApplicationContext().getResources().updateConfiguration(config, null);		
	}

}

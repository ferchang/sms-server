package hmz2627_at_gmail_dot_com.sms_server;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}
	@Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(!key.equals("lang")) return;
		MyLocale.changeLocale(this, Prefs.p.getString("lang", "default"));
		recreate();
    }
	@Override
    public void onResume() {
        super.onResume();
		Current.currentActivity=this;
	}
}

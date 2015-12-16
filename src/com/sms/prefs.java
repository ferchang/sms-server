package com.sms;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class prefs extends PreferenceActivity {
@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
addPreferencesFromResource(R.xml.prefs);
}
}
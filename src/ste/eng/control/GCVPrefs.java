package ste.eng.control;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class GCVPrefs extends PreferenceActivity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.prefs);
	}
}


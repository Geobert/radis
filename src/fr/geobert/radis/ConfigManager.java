package fr.geobert.radis;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class ConfigManager extends PreferenceActivity {
	public final static String INSERTION_DATE = "insertion_date";
	public final static String SHARED_PREF_NAME = "RadisPrefs";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		Preference pref = (Preference) findPreference(INSERTION_DATE);
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				SharedPreferences customSharedPreference = getSharedPreferences(
						SHARED_PREF_NAME, Activity.MODE_PRIVATE);
				SharedPreferences.Editor editor = customSharedPreference.edit();
				editor.putInt(INSERTION_DATE, Integer.valueOf((String)newValue));
				editor.commit();
				return true;
			}
		});
		//init();
	}

	
}

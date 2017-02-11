package com.tremendo.uitweakforgboard;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.preference.*;
import android.text.*;
import android.text.style.*;
import android.view.*;
import android.widget.*;


public class MainActivity extends Activity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment())
				.commit();
		}
    }


	public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

		private SharedPreferences mSharedPrefs;
		private boolean mXposedActivated = false;


		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);	
			getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
			mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		}


		@Override
		public void onResume() {
			/* Xposed module hooks itself here to set 'mXposedActivated' to true */
			super.onResume();
			mSharedPrefs.registerOnSharedPreferenceChangeListener(this);
			checkXposed();
			checkPreferenceEnabled();
		}


		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
			if ("pref_key_xposed".equals(preference.getKey())) {
				launchXposed();
			}
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		}


		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, final String prefKey) {
			if ("pref_hide_search_bar".equals(prefKey)) {
				CheckBoxPreference searchBarPreference = (CheckBoxPreference) getPreferenceManager().findPreference(prefKey);
				Intent intent = new Intent(MainActivity.class.getPackage().getName()+".TOGGLE_SEARCH_BAR");
				intent.putExtra("hide", searchBarPreference.isChecked());
				getActivity().sendBroadcast(intent);
			}
		}


		@Override
		public void onPause() {
			super.onPause();
			mSharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
		}


		private void checkXposed() {
			Preference xposedAlertPref = findPreference("pref_key_xposed");
			if (mXposedActivated && xposedAlertPref != null) {
				getPreferenceScreen().removePreference(xposedAlertPref);
			} else if (xposedAlertPref != null) {
				xposedAlertPref.setTitle(colorizeText(Color.RED, xposedAlertPref.getTitle()));
			}
		}


		private void checkPreferenceEnabled() {
			Preference searchbarPreference = getPreferenceManager().findPreference("pref_hide_search_bar");
			searchbarPreference.setEnabled(mXposedActivated && isGboardInstalled());
			if (isGboardInstalled()) {
				searchbarPreference.setSummary(null);
			} else searchbarPreference.setSummary(colorizeText(Color.RED, "Gboard not installed"));
		}	


		private boolean isGboardInstalled() {
			try {
				ApplicationInfo appInfo = getActivity().getPackageManager().getApplicationInfo("com.google.android.inputmethod.latin", 0);
				return appInfo.enabled;
			} catch (PackageManager.NameNotFoundException e) {
				return false;
			}
		}


		public static CharSequence colorizeText(Object color, CharSequence text) {
			if (text != null) {
				SpannableString span = new SpannableString(text);
				span.setSpan(new ForegroundColorSpan(color), 0, text.length(), 0);
				return span;
			}
			return text;
		}


		private void launchXposed() {
			Intent intent = new Intent("de.robv.android.xposed.installer.ModulesBookmark");
			intent.setPackage("de.robv.android.xposed.installer");
			intent.putExtra("section", "modules");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				intent = getActivity().getPackageManager().getLaunchIntentForPackage("de.robv.android.xposed.installer");
				if (intent != null) {
					startActivity(intent);
				} else Toast.makeText(getContext(), "Xposed Installer not detected", 0).show();
			}
		}

	}

}

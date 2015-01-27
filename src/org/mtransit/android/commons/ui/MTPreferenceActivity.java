package org.mtransit.android.commons.ui;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTPreferenceActivity extends PreferenceActivity implements MTLog.Loggable {

	@Deprecated
	@Override
	public void addPreferencesFromResource(int preferencesResId) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "addPreferencesFromResource(%s)", preferencesResId);
		}
		super.addPreferencesFromResource(preferencesResId);
	}

	@Deprecated
	@Override
	public Preference findPreference(CharSequence key) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "findPreference(%s)", key);
		}
		return super.findPreference(key);
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPreferenceStartFragment(%s,%s)", caller, pref);
		}
		return super.onPreferenceStartFragment(caller, pref);
	}

	@Override
	public void startPreferenceFragment(Fragment fragment, boolean push) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "startPreferenceFragment(%s,%s)", fragment, push);
		}
		super.startPreferenceFragment(fragment, push);
	}

	@Override
	public void startWithFragment(String fragmentName, Bundle args, Fragment resultTo, int resultRequestCode, int titleRes, int shortTitleRes) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "startWithFragment(%s,%s,%s,%s,%s,%s)", fragmentName, args, resultTo, resultRequestCode, titleRes, shortTitleRes);
		}
		super.startWithFragment(fragmentName, args, resultTo, resultRequestCode, titleRes, shortTitleRes);
	}

	@Deprecated
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPreferenceTreeClick(%s,%s)", preferenceScreen, preference);
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Deprecated
	@Override
	public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "setPreferenceScreen(%s)", preferenceScreen);
		}
		super.setPreferenceScreen(preferenceScreen);
	}

	// INHERITED FROM ACTIVITY

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreate(%s)", savedInstanceState);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onNewIntent(%s)", intent);
		}
		super.onNewIntent(intent);
	}

	@Override
	protected void onRestart() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onRestart()");
		}
		super.onRestart();
	}

	@Override
	protected void onStart() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStart()");
		}
		super.onStart();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onRestoreInstanceState(%s)", savedInstanceState);
		}
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onResume()");
		}
		super.onResume();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onWindowFocusChanged(%s)", hasFocus);
		}
		super.onWindowFocusChanged(hasFocus);
	}

	@Override
	protected void onPostResume() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPostResume()");
		}
		super.onPostResume();
	}

	@Override
	protected void onPause() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPause()");
		}
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onSaveInstanceState(%s)", outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStop()");
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroy()");
		}
		super.onDestroy();
	}
}

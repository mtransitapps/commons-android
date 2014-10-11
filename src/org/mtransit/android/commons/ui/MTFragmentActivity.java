package org.mtransit.android.commons.ui;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTFragmentActivity extends FragmentActivity implements MTLog.Loggable {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreate(%s)", savedInstanceState);
		}
		super.onCreate(savedInstanceState);
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

package org.mtransit.android.commons.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LinkUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.ToastUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

@SuppressLint("Registered")
public class ModuleRedirectActivity extends Activity implements MTLog.Loggable {

	@NonNull
	@Override
	public String getLogTag() {
		return ModuleRedirectActivity.class.getSimpleName();
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (PackageManagerUtils.isAppInstalled(this, Constants.MAIN_APP_PACKAGE_NAME)) {
			ToastUtils.makeTextAndShowCentered(this, R.string.opening_main_app_and_removing_icon, Toast.LENGTH_SHORT);
			PackageManagerUtils.removeModuleLauncherIcon(this);
			PackageManagerUtils.openApp(this, Constants.MAIN_APP_PACKAGE_NAME, Intent.FLAG_ACTIVITY_CLEAR_TOP);
		} else {
			ToastUtils.makeTextAndShowCentered(this, R.string.please_install_main_app_from_google_play, Toast.LENGTH_LONG);
			StoreUtils.viewAppPage(this, Constants.MAIN_APP_PACKAGE_NAME, LinkUtils.NO_LABEL);
		}
		finish();
	}
}

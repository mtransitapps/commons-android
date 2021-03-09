package org.mtransit.android.commons.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.ToastUtils;

@SuppressLint("Registered")
public class ModuleDeviceNotSupportedActivity extends Activity implements MTLog.Loggable {

	@NonNull
	@Override
	public String getLogTag() {
		return ModuleDeviceNotSupportedActivity.class.getSimpleName();
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isDeviceSupported()) {
			PackageManagerUtils.resetModuleLauncherIcon(this);
			ToastUtils.makeTextAndShowCentered(this, R.string.please_uninstall_app_device_not_supported, Toast.LENGTH_LONG);
			PackageManagerUtils.uninstall(this, this);
		}
		finish();
	}

	@SuppressLint("ObsoleteSdkInt")
	private boolean isDeviceSupported() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN;
	}
}

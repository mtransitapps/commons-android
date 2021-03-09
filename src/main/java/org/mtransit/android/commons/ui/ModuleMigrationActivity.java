package org.mtransit.android.commons.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.LinkUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.ToastUtils;

@SuppressLint("Registered")
public class ModuleMigrationActivity extends Activity implements MTLog.Loggable {

	@NonNull
	@Override
	public String getLogTag() {
		return ModuleMigrationActivity.class.getSimpleName();
	}

	@Nullable
	private static String fromPkg = null;

	/**
	 * Override if multiple {@link ModuleMigrationActivity} implementations in same app.
	 */
	@NonNull
	private static String getFROM_PKG(@NonNull Context context) {
		if (fromPkg == null) {
			fromPkg = context.getResources().getString(R.string.module_migration_from_pkg);
		}
		return fromPkg;
	}

	@Nullable
	private static String toPkg = null;

	/**
	 * Override if multiple {@link ModuleMigrationActivity} implementations in same app.
	 */
	@NonNull
	private static String getTO_PKG(@NonNull Context context) {
		if (toPkg == null) {
			toPkg = context.getResources().getString(R.string.module_migration_to_pkg);
		}
		return toPkg;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (PackageManagerUtils.isAppInstalled(this, getTO_PKG(this))) {
			ToastUtils.makeTextAndShowCentered(this, R.string.please_uninstall_old_app, Toast.LENGTH_LONG);
			PackageManagerUtils.uninstallApp(this, getFROM_PKG(this));
		} else {
			ToastUtils.makeTextAndShowCentered(this, R.string.please_install_new_app_from_google_play, Toast.LENGTH_LONG);
			StoreUtils.viewAppPage(this, getTO_PKG(this), LinkUtils.NO_LABEL);
		}
		finish();
	}
}

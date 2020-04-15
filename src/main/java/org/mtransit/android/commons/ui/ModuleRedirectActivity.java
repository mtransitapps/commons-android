package org.mtransit.android.commons.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LinkUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.provider.AgencyProviderContract;

@SuppressLint("Registered")
public class ModuleRedirectActivity extends Activity implements MTLog.Loggable {

	private static final String LOG_TAG = ModuleRedirectActivity.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	View rootView;
	TextView appInstalledTv;
	Button openDownloadButton;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_module_redirect);

		this.rootView = findViewById(R.id.module_installed_root);
		this.appInstalledTv = findViewById(R.id.module_installed_text);
		this.openDownloadButton = findViewById(R.id.module_open_download_app);

		this.openDownloadButton.setOnClickListener(v ->
				onButtonClicked()
		);

		initAgencyData();
		ping();
	}

	private void initAgencyData() {
		String bgColor = "6699FF"; // DEFAULT
		String appInstalledText = getString(R.string.congratulations_module_app_installed_default); // DEFAULT
		String authority = findProviderAuthority();
		if (authority != null) {
			Cursor cursor = null;
			try {
				Uri authorityUri = UriUtils.newContentUri(authority);
				Uri uri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.ALL_PATH);
				cursor = getContentResolver().query(uri, null, null, null, null);
				if (cursor != null && cursor.getCount() > 0) {
					if (cursor.moveToFirst()) {
						String longName = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.LABEL_PATH));
						appInstalledText = getString(R.string.congratulations_module_app_installed_and_module, longName);
						bgColor = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.COLOR_PATH));
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error!");
			} finally {
				SqlUtils.closeQuietly(cursor);
			}
		}
		this.appInstalledTv.setText(appInstalledText);
		this.rootView.setBackgroundColor(ColorUtils.parseColor(bgColor));
	}

	private void ping() {
		String authority = findProviderAuthority();
		if (authority != null) {
			Cursor cursor = null;
			try {
				Uri authorityUri = UriUtils.newContentUri(authority);
				Uri pingUri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.PING_PATH);
				cursor = getContentResolver().query(pingUri, null, null, null, null);
			} catch (Exception e) {
				MTLog.w(this, e, "Error!");
			} finally {
				SqlUtils.closeQuietly(cursor);
			}
		}
	}

	@Nullable
	private String findProviderAuthority() {
		String agencyProviderMetaData = getString(R.string.agency_provider);
		ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(this, getPackageName());
		if (providers != null) {
			for (ProviderInfo provider : providers) {
				if (provider.metaData != null) {
					if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
						return provider.authority;
					}
				}
			}
		}
		return null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.openDownloadButton.setText(PackageManagerUtils.isAppInstalled(this, Constants.MAIN_APP_PACKAGE_NAME) ?
				R.string.action_open_main_app :
				R.string.action_download_main_app);
	}

	private void onButtonClicked() {
		checkKeepTempIcon();
		if (PackageManagerUtils.isAppInstalled(this, Constants.MAIN_APP_PACKAGE_NAME)) {
			PackageManagerUtils.openApp(this, Constants.MAIN_APP_PACKAGE_NAME, Intent.FLAG_ACTIVITY_CLEAR_TOP);
		} else {
			StoreUtils.viewAppPage(this, Constants.MAIN_APP_PACKAGE_NAME, LinkUtils.NO_LABEL);
		}
		finish();
	}

	private void checkKeepTempIcon() {
		CheckBox keepTempIconCb = findViewById(R.id.module_keep_temp_icon);
		boolean keepTempIcon = keepTempIconCb.isChecked();
		PreferenceUtils.savePrefLcl(this, PreferenceUtils.PREFS_KEEP_MODULE_APP_LAUNCHER_ICON, keepTempIcon, false);
		if (keepTempIcon) {
			PackageManagerUtils.resetModuleLauncherIcon(this);
		} else {
			PackageManagerUtils.removeModuleLauncherIcon(this);
		}
	}
}

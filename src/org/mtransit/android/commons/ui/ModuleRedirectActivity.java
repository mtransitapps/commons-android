package org.mtransit.android.commons.ui;

import java.util.Arrays;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LinkUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.provider.AgencyProviderContract;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

@SuppressLint("Registered")
public class ModuleRedirectActivity extends Activity implements MTLog.Loggable {

	private static final String LOG_TAG = ModuleRedirectActivity.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_module_redirect);

		View rootView = findViewById(R.id.module_installed_root);
		TextView textView = findViewById(R.id.module_installed_text);
		Button button = findViewById(R.id.module_open_download_app);

		String agencyProviderMetaData = getString(R.string.agency_provider);
		String agencyProviderTypeMetaData = getString(R.string.agency_provider_type);
		String rtsProviderMetaData = getString(R.string.rts_provider);
		ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(this, getPackageName());
		if (providers != null) {
			for (ProviderInfo provider : providers) {
				if (provider.metaData != null) {
					if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
						String authority = provider.authority;
						int agencyTypeId = provider.metaData.getInt(agencyProviderTypeMetaData, -1);
						boolean isRTS = rtsProviderMetaData.equals(provider.metaData.getString(rtsProviderMetaData));
						Cursor cursor = null;
						try {
							Uri authorityUri = UriUtils.newContentUri(authority);
							Uri uri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.ALL_PATH);
							cursor = getContentResolver().query(uri, null, null, null, null);
							if (cursor != null && cursor.getCount() > 0) {
								if (cursor.moveToFirst()) {
									String shortName = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.SHORT_NAME_PATH));
									String longName = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.LABEL_PATH));
									textView.setText(getString(R.string.congratulations_module_app_installed_and_module, longName));
									String color = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.COLOR_PATH));
									int colorRes = ColorUtils.parseColor(color);
									rootView.setBackgroundColor(colorRes);
								}
							}
						} catch (Exception e) {
							MTLog.w(this, e, "Error!");
						} finally {
							SqlUtils.closeQuietly(cursor);
						}
					}
				}
			}
		}
		button.setText(PackageManagerUtils.isAppInstalled(this, Constants.MAIN_APP_PACKAGE_NAME) ?
				R.string.action_open_main_app :
				R.string.action_download_main_app);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onButtonClicked();
			}
		});
	}

	private void onButtonClicked() {
		if (PackageManagerUtils.isAppInstalled(this, Constants.MAIN_APP_PACKAGE_NAME)) {
			PackageManagerUtils.openApp(this, Constants.MAIN_APP_PACKAGE_NAME, Intent.FLAG_ACTIVITY_CLEAR_TOP);
		} else {
			StoreUtils.viewAppPage(this, Constants.MAIN_APP_PACKAGE_NAME, LinkUtils.NO_LABEL);
		}
		finish();
	}
}

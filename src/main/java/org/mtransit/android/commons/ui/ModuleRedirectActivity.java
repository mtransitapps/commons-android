package org.mtransit.android.commons.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.mtransit.android.commons.BuildConfig;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LinkUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.provider.AgencyProviderContract;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.commons.FeatureFlags;

import java.util.concurrent.TimeUnit;

@SuppressLint("Registered")
public class ModuleRedirectActivity extends Activity implements MTLog.Loggable {

	private static final String LOG_TAG = ModuleRedirectActivity.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final long COUNT_DOWN_DURATION = TimeUnit.SECONDS.toMillis(BuildConfig.DEBUG ? 3L : 10L);
	private static final long COUNT_DOWN_STEPS = TimeUnit.SECONDS.toMillis(1L);

	private static final boolean SKIP_IF_INSTALLED = true;
	// private static final boolean SKIP_IF_INSTALLED = !BuildConfig.DEBUG; // DEBUG

	private static final String COUNT_DOWN_CANCELLED = "count_down_cancelled";
	private static final boolean COUNT_DOWN_CANCELLED_DEFAULT = false;
	// private static final boolean COUNT_DOWN_CANCELLED_DEFAULT = true; // DEBUG

	private static final String PRIVACY_POLICY_PAGE_URL = "https://mtransitapps.github.io/privacy/";
	private static final String PRIVACY_POLICY_FR_PAGE_URL = "https://mtransitapps.github.io/privacy/fr";

	@Nullable
	private View rootView;
	@Nullable
	private ImageView typeImg;
	@Nullable
	private TextView typeDownArrow;
	@Nullable
	private TextView appInstalledTv;
	@SuppressWarnings("FieldCanBeLocal")
	@Nullable
	private ImageView appIconImg;
	@Nullable
	private TextView whyDescriptionTv;
	@Nullable
	private TextView openDownloadTitle;
	@Nullable
	private Button openDownloadButton;
	@Nullable
	private View getItOnGooglePlay;
	@Nullable
	private TextView countdownText;
	@Nullable
	private TextView countdownCancelText;
	@Nullable
	private TextView privacyPolicyLink;

	private boolean countDownCancelled = COUNT_DOWN_CANCELLED_DEFAULT;

	@NonNull
	private final CountDownTimer countDownTimer = new CountDownTimer(COUNT_DOWN_DURATION, COUNT_DOWN_STEPS) {

		public void onTick(long millisUntilFinished) {
			onCountDownTimeStateChanged(millisUntilFinished);
		}

		public void onFinish() {
			if (!FeatureFlags.F_MODULE_AUTO_OPEN) {
				return;
			}
			onButtonClicked();
		}
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (SKIP_IF_INSTALLED) {
			if (isMainAppInstalled()) {
				openMainApp();
				finish();
				return;
			}
		}
		setTheme(R.style.ModuleBaseTheme);
		setContentView(R.layout.activity_module_redirect);

		this.rootView = findViewById(R.id.module_installed_root);
		this.typeImg = findViewById(R.id.type_img);
		this.typeDownArrow = findViewById(R.id.type_down_arrow);
		this.appInstalledTv = findViewById(R.id.module_installed_text);
		this.appIconImg = findViewById(R.id.app_icon);
		this.whyDescriptionTv = findViewById(R.id.why_description);
		this.openDownloadTitle = findViewById(R.id.module_open_download_app_title);
		this.openDownloadButton = findViewById(R.id.module_open_download_app_button);
		this.getItOnGooglePlay = findViewById(R.id.get_it_on_google_play);
		this.countdownText = findViewById(R.id.module_countdown_text);
		this.countdownCancelText = findViewById(R.id.module_countdown_cancel_text);
		this.privacyPolicyLink = findViewById(R.id.module_privacy_policy_link);

		if (FeatureFlags.F_MODULE_AUTO_OPEN) {
			if (savedInstanceState != null) {
				this.countDownCancelled = savedInstanceState.getBoolean(COUNT_DOWN_CANCELLED, COUNT_DOWN_CANCELLED_DEFAULT);
			}
		}

		if (this.appIconImg != null) {
			this.appIconImg.setOnClickListener(v ->
					onButtonClicked()
			);
		}
		if (this.openDownloadTitle != null) {
			this.openDownloadTitle.setOnClickListener(v ->
					onButtonClicked()
			);
		}
		if (this.openDownloadButton != null) {
			this.openDownloadButton.setOnClickListener(v ->
					onButtonClicked()
			);
		}
		if (this.getItOnGooglePlay != null) {
			this.getItOnGooglePlay.setOnClickListener(v ->
					onButtonClicked()
			);
		}

		setupPrivacyPolicyLink();

		initAgencyData();
		TaskUtils.execute(new PingTask(getApplication()));
	}

	private void setupPrivacyPolicyLink() {
		if (this.privacyPolicyLink != null) {
			final SpannableString privacyPolicyLink = new SpannableString(getString(R.string.privacy_policy));
			privacyPolicyLink.setSpan(new UnderlineSpan(), 0, privacyPolicyLink.length(), 0);
			this.privacyPolicyLink.setText(privacyPolicyLink);
			this.privacyPolicyLink.setOnClickListener(v -> {
				final String url = LocaleUtils.isFR() ? PRIVACY_POLICY_FR_PAGE_URL : PRIVACY_POLICY_PAGE_URL;
				final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(browserIntent);
			});
			this.privacyPolicyLink.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (FeatureFlags.F_MODULE_AUTO_OPEN) {
			if (event != null && event.getAction() == MotionEvent.ACTION_UP) {
				this.countDownCancelled = true;
				this.countDownTimer.cancel();
				onCountDownCancelStateChanged();
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (FeatureFlags.F_MODULE_AUTO_OPEN) {
			if (hasFocus) {
				onCountDownCancelStateChanged(); // force refresh
				if (!this.countDownCancelled) {
					this.countDownTimer.start(); // "resume" (actually restarts)
				}
			} else {
				this.countDownTimer.cancel();
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static class PingTask extends MTCancellableAsyncTask<Void, Void, Void> {

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		@NonNull
		private final Application appContext;

		PingTask(@NonNull Application appContext) {
			this.appContext = appContext;
		}

		@Nullable
		@Override
		protected Void doInBackgroundNotCancelledMT(@Nullable Void... voids) {
			ping();
			return null;
		}

		private void ping() {
			final Pair<String, Integer> authorityAndType = findProviderAuthorityAndType(this.appContext);
			final String authority = authorityAndType.first;
			final Integer type = authorityAndType.second;
			if (type != null) {
				if (FeatureFlags.F_PROVIDER_DEPLOY_SYNC_GTFS_ONLY) {
					if (!DataSourceTypeId.isGTFSType(type)) {
						return;
					}
				}
			}
			if (authority != null) {
				Cursor cursor = null;
				try {
					Uri authorityUri = UriUtils.newContentUri(authority);
					Uri pingUri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.PING_PATH);
					cursor = this.appContext.getContentResolver().query(pingUri, null, null, null, null);
				} catch (Exception e) {
					MTLog.w(this, e, "Error!");
				} finally {
					SqlUtils.closeQuietly(cursor);
				}
			}
		}
	}

	private void initAgencyData() {
		String bgColor = "6699FF"; // DEFAULT
		String appInstalledText = getString(R.string.module_app_installed_default); // DEFAULT
		final Pair<String, Integer> authorityAndType = findProviderAuthorityAndType(this);
		final String authority = authorityAndType.first;
		final Integer type = authorityAndType.second;
		if (authority != null) {
			Cursor cursor = null;
			try {
				final Uri authorityUri = UriUtils.newContentUri(authority);
				final Uri uri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.ALL_PATH);
				cursor = getContentResolver().query(uri, null, null, null, null);
				if (cursor != null && cursor.getCount() > 0) {
					if (cursor.moveToFirst()) {
						String longName = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.LABEL_PATH));
						appInstalledText = getString(R.string.module_app_installed_and_module, longName);
						bgColor = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.COLOR_PATH));
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error!");
			} finally {
				SqlUtils.closeQuietly(cursor);
			}
		}
		boolean typeSet = false;
		if (type != null && this.typeImg != null) {
			switch (type) {
			case DataSourceTypeId.LIGHT_RAIL:
				this.typeImg.setImageResource(R.drawable.ic_light_rail_black_24dp);
				typeSet = true;
				break;
			case DataSourceTypeId.SUBWAY:
				this.typeImg.setImageResource(R.drawable.ic_directions_subway_black_24dp);
				typeSet = true;
				break;
			case DataSourceTypeId.RAIL:
				this.typeImg.setImageResource(R.drawable.ic_directions_railway_black_24dp);
				typeSet = true;
				break;
			case DataSourceTypeId.FERRY:
				this.typeImg.setImageResource(R.drawable.ic_directions_boat_black_24dp);
				typeSet = true;
				break;
			case DataSourceTypeId.BIKE:
				this.typeImg.setImageResource(R.drawable.ic_directions_bike_black_24dp);
				typeSet = true;
				break;
			case DataSourceTypeId.BUS:
				this.typeImg.setImageResource(R.drawable.ic_directions_bus_black_24dp);
				typeSet = true;
				break;
			}
		}
		if (this.typeImg != null) {
			this.typeImg.setVisibility(typeSet ? View.VISIBLE : View.GONE);
		}
		if (this.typeDownArrow != null) {
			this.typeDownArrow.setVisibility(typeSet ? View.VISIBLE : View.GONE);
		}
		if (this.appInstalledTv != null) {
			this.appInstalledTv.setText(HtmlUtils.fromHtml(appInstalledText));
		}
		if (this.whyDescriptionTv != null) {
			this.whyDescriptionTv.setText(HtmlUtils.fromHtml(getString(R.string.why_multiple_apps)));
		}
		if (this.rootView != null) {
			this.rootView.setBackgroundColor(ColorUtils.parseColor(bgColor));
		}
	}

	@NonNull
	private static Pair<String, Integer> findProviderAuthorityAndType(@NonNull Context context) {
		final String agencyProviderMetaData = context.getString(R.string.agency_provider);
		final String agencyProviderTypeMetaData = context.getString(R.string.agency_provider_type);
		final ProviderInfo[] providers = PackageManagerUtils.findContentProvidersWithMetaData(context, context.getPackageName());
		String providerAuthority = null;
		Integer providerType = null;
		if (providers != null) {
			for (ProviderInfo provider : providers) {
				if (provider.metaData != null) {
					if (agencyProviderMetaData.equals(provider.metaData.getString(agencyProviderMetaData))) {
						providerAuthority = provider.authority;
						providerType = provider.metaData.getInt(agencyProviderTypeMetaData, -1);
					}

				}
			}
		}
		return new Pair<>(providerAuthority, providerType);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (FeatureFlags.F_MODULE_AUTO_OPEN) {
			outState.putBoolean(COUNT_DOWN_CANCELLED, this.countDownCancelled);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (FeatureFlags.F_MODULE_AUTO_OPEN) {
			this.countDownCancelled = savedInstanceState.getBoolean(COUNT_DOWN_CANCELLED, COUNT_DOWN_CANCELLED_DEFAULT);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.openDownloadTitle != null) {
			this.openDownloadTitle.setText(isMainAppInstalled() ?
					R.string.action_open_main_app :
					R.string.action_download_main_app);
			this.openDownloadTitle.setVisibility(isMainAppInstalled() ?
					View.GONE :
					View.VISIBLE);
		}
		if (this.openDownloadButton != null) {
			this.openDownloadButton.setText(isMainAppInstalled() ?
					R.string.action_open_main_app :
					R.string.action_download_main_app);
			this.openDownloadButton.setVisibility(isMainAppInstalled() ?
					View.VISIBLE :
					View.GONE);
		}
		if (this.getItOnGooglePlay != null) {
			this.getItOnGooglePlay.setVisibility(isMainAppInstalled() ?
					View.GONE :
					View.VISIBLE);
		}
	}

	private boolean isMainAppInstalled() {
		return PackageManagerUtils.isAppInstalled(this, Constants.MAIN_APP_PACKAGE_NAME);
	}

	private void onCountDownTimeStateChanged(long millisUntilFinished) {
		if (!FeatureFlags.F_MODULE_AUTO_OPEN) {
			return;
		}
		if (this.countdownText != null) {
			final int seconds = Math.round(millisUntilFinished / 1000.0f);
			this.countdownText.setText(
					getResources().getQuantityString(R.plurals.text_opening_main_app_in_and_seconds, seconds, seconds)
			);
		}
	}

	private void onCountDownCancelStateChanged() {
		if (!FeatureFlags.F_MODULE_AUTO_OPEN) {
			return;
		}
		if (this.countDownCancelled) {
			if (this.countdownText != null) {
				this.countdownText.setVisibility(View.INVISIBLE);
			}
			if (this.countdownCancelText != null) {
				this.countdownCancelText.setVisibility(View.INVISIBLE);
			}
		} else {
			if (this.countdownText != null) {
				this.countdownText.setVisibility(View.VISIBLE);
			}
			if (this.countdownCancelText != null) {
				this.countdownCancelText.setVisibility(View.VISIBLE);
			}
		}
	}

	private void onButtonClicked() {
		checkKeepTempIcon();
		if (isMainAppInstalled()) {
			// if (Constants.DEBUG) { // FIXME not working
			// PackageManagerUtils.openApp(this, Constants.MAIN_APP_PACKAGE_NAME, Intent.FLAG_ACTIVITY_CLEAR_TOP,
			// Intent.FLAG_ACTIVITY_NEW_TASK, Intent.FLAG_ACTIVITY_SINGLE_TOP);
			// } else {
			openMainApp();
			// }
		} else {
			StoreUtils.viewAppPage(this, Constants.MAIN_APP_PACKAGE_NAME, LinkUtils.NO_LABEL);
		}
		finish();
	}

	private void openMainApp() {
		PackageManagerUtils.openApp(this, Constants.MAIN_APP_PACKAGE_NAME,
				Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK
		);
	}

	private void checkKeepTempIcon() {
		final CheckBox keepTempIconCb = findViewById(R.id.module_keep_temp_icon);
		final boolean keepTempIcon = keepTempIconCb.isChecked();
		PreferenceUtils.savePrefLclAsync(this, PreferenceUtils.PREFS_KEEP_MODULE_APP_LAUNCHER_ICON, keepTempIcon);
		if (keepTempIcon) {
			PackageManagerUtils.resetLauncherIcon(this);
		} else {
			PackageManagerUtils.removeLauncherIcon(this);
		}
	}
}

package org.mtransit.android.commons.ui;

import static org.mtransit.android.commons.ui.ModuleRedirectActivityExtKt.loadProvidersInfo;
import static org.mtransit.android.commons.ui.ModuleRedirectActivityExtKt.updateAgencyInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import org.mtransit.android.commons.BatteryUtils;
import org.mtransit.android.commons.BuildConfig;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.LinkUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerExtKt;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.ResourceExtKt;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StoreUtils;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.DataSourceTypeId;
import org.mtransit.android.commons.databinding.ActivityModuleRedirectBinding;
import org.mtransit.android.commons.provider.agency.AgencyProviderContract;
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

	private @Nullable ActivityModuleRedirectBinding binding;

	protected @Nullable String agencyProviderAuthority = null;
	protected @Nullable Integer agencyProviderType = null;
	protected @Nullable Boolean hasRDSProvider = null;
	protected @Nullable Boolean hasScheduleProvider = null;
	protected @Nullable Boolean hasNonStaticStatusProvider = null;
	protected @Nullable Boolean hasServiceUpdateProvider = null;
	protected @Nullable Boolean hasVehicleLocationProvider = null;
	protected @Nullable Boolean hasNewsProvider = null;

	private boolean countDownCancelled = COUNT_DOWN_CANCELLED_DEFAULT;

	@NonNull
	private final CountDownTimer countDownTimer = new CountDownTimer(COUNT_DOWN_DURATION, COUNT_DOWN_STEPS) {

		public void onTick(long millisUntilFinished) {
			onCountDownTimeStateChanged(millisUntilFinished);
		}

		public void onFinish() {
			if (!FeatureFlags.F_MODULE_AUTO_OPEN) return;
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
		WindowCompat.enableEdgeToEdge(getWindow());
		final ActivityModuleRedirectBinding binding = ActivityModuleRedirectBinding.inflate(getLayoutInflater());
		this.binding = binding;
		setContentView(binding.getRoot());

		final int initialLeft = binding.getRoot().getPaddingLeft();
		final int initialTop = binding.getRoot().getPaddingTop();
		final int initialRight = binding.getRoot().getPaddingRight();
		final int initialBottom = binding.getRoot().getPaddingBottom();
		ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
			final Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			view.setPadding(
					initialLeft + systemBars.left,
					initialTop + systemBars.top,
					initialRight + systemBars.right,
					initialBottom + systemBars.bottom
			);
			return insets;
		});

		if (FeatureFlags.F_MODULE_AUTO_OPEN) {
			if (savedInstanceState != null) {
				this.countDownCancelled = savedInstanceState.getBoolean(COUNT_DOWN_CANCELLED, COUNT_DOWN_CANCELLED_DEFAULT);
			}
		}

		binding.appIcon.setOnClickListener(v -> onButtonClicked());
		binding.moduleOpenDownloadAppTitle.setOnClickListener(v -> onButtonClicked());
		binding.moduleOpenDownloadAppButton.setOnClickListener(v -> onButtonClicked());
		binding.getItOnGooglePlay.setOnClickListener(v -> onButtonClicked());

		setupPrivacyPolicyLink();

		initAgencyData(binding);

		if (isMainAppInstalled() && BatteryUtils.shouldUseBatteryForOptionalWork(this)) {
			TaskUtils.execute(new PingTask(getApplication(), this.agencyProviderAuthority, this.agencyProviderType)); // LAST
		}
	}

	private void setupPrivacyPolicyLink() {
		if (this.binding == null) return;
		final SpannableString privacyPolicyLink = new SpannableString(getString(R.string.privacy_policy));
		privacyPolicyLink.setSpan(new UnderlineSpan(), 0, privacyPolicyLink.length(), 0);
		binding.modulePrivacyPolicyLink.setText(privacyPolicyLink);
		binding.modulePrivacyPolicyLink.setOnClickListener(v -> {
			final String url = LocaleUtils.isFR() ? PRIVACY_POLICY_FR_PAGE_URL : PRIVACY_POLICY_PAGE_URL;
			final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			try {
				startActivity(browserIntent);
			} catch (Exception e) {
				MTLog.w(this, e, "Failed to open privacy policy URL: %s", url);
			}
		});
		binding.modulePrivacyPolicyLink.setVisibility(View.VISIBLE);
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
		@Nullable
		private final String agencyProviderAuthority;
		@Nullable
		private final Integer agencyProviderType;

		PingTask(@NonNull Application appContext, @Nullable String agencyProviderAuthority, @Nullable Integer agencyProviderType) {
			this.appContext = appContext;
			this.agencyProviderAuthority = agencyProviderAuthority;
			this.agencyProviderType = agencyProviderType;
		}

		@Nullable
		@Override
		protected Void doInBackgroundNotCancelledMT(@Nullable Void... voids) {
			ping();
			return null;
		}

		private void ping() {
			if (this.agencyProviderType != null) {
				if (FeatureFlags.F_PROVIDER_DEPLOY_SYNC_GTFS_ONLY) {
					if (!DataSourceTypeId.isGTFSType(this.agencyProviderType)) {
						return;
					}
				}
			}
			if (this.agencyProviderAuthority != null) {
				Cursor cursor = null;
				try {
					final Uri authorityUri = UriUtils.newContentUri(this.agencyProviderAuthority);
					final Uri pingUri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.PING_PATH);
					cursor = this.appContext.getContentResolver().query(pingUri, null, null, null, null);
				} catch (Exception e) {
					MTLog.w(this, e, "Error!");
				} finally {
					SqlUtils.closeQuietly(cursor);
				}
			}
		}
	}

	private void initAgencyData(@NonNull ActivityModuleRedirectBinding binding) {
		String bgColor = "6699FF"; // DEFAULT
		CharSequence appInstalledText = getResources().getText(R.string.module_app_installed_default); // DEFAULT
		loadProvidersInfo(this);
		if (this.agencyProviderAuthority != null) {
			Cursor cursor = null;
			try {
				final Uri authorityUri = UriUtils.newContentUri(this.agencyProviderAuthority);
				final Uri uri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.ALL_PATH); // agency name & color
				cursor = getContentResolver().query(uri, null, null, null, null);
				if (cursor != null && cursor.getCount() > 0) {
					if (cursor.moveToFirst()) {
						final String shortName = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.SHORT_NAME_PATH));
						appInstalledText = ResourceExtKt.getText(getResources(), R.string.module_app_installed_and_module, shortName);
						bgColor = cursor.getString(cursor.getColumnIndexOrThrow(AgencyProviderContract.COLOR_PATH));
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error!");
			} finally {
				SqlUtils.closeQuietly(cursor);
			}
		}
		@Nullable @DrawableRes Integer typeImgResId = null;
		if (this.agencyProviderType != null) {
			switch (this.agencyProviderType) {
			case DataSourceTypeId.LIGHT_RAIL:
				typeImgResId = R.drawable.ic_light_rail_black_24dp;
				break;
			case DataSourceTypeId.SUBWAY:
				typeImgResId = R.drawable.ic_directions_subway_black_24dp;
				break;
			case DataSourceTypeId.RAIL:
				typeImgResId = R.drawable.ic_directions_railway_black_24dp;
				break;
			case DataSourceTypeId.FERRY:
				typeImgResId = R.drawable.ic_directions_boat_black_24dp;
				break;
			case DataSourceTypeId.BIKE:
				typeImgResId = R.drawable.ic_directions_bike_black_24dp;
				break;
			case DataSourceTypeId.BUS:
				typeImgResId = R.drawable.ic_directions_bus_black_24dp;
				break;
			}
		}
		if (typeImgResId != null) {
			final Drawable typeImgDrawable = ContextCompat.getDrawable(this, typeImgResId);
			if (typeImgDrawable != null) {
				final int typeImgSize = getResources().getDimensionPixelSize(R.dimen.module_redirect_app_icon_size);
				typeImgDrawable.setBounds(0, 0, typeImgSize, typeImgSize);
				binding.moduleInstalledText.setCompoundDrawablesRelative(
						typeImgDrawable, // start
						null,
						null,
						null
				);
			} else {
				binding.moduleInstalledText.setCompoundDrawablesRelativeWithIntrinsicBounds(
						typeImgResId, // start
						0, 0, 0
				);
			}
		}
		binding.typeDownArrow.setVisibility(typeImgResId != null ? View.VISIBLE : View.GONE);
		binding.moduleInstalledText.setText(appInstalledText);
		updateAgencyInfo(this, binding);
		binding.getRoot().setBackgroundColor(ColorUtils.parseColor(bgColor));
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
		if (this.binding != null) {
			updateUIState(this.binding);
		}
	}

	private void updateUIState(@NonNull ActivityModuleRedirectBinding binding) {
		binding.moduleOpenDownloadAppTitle.setText(isMainAppInstalled() ?
				R.string.action_open_main_app :
				R.string.action_download_main_app);
		binding.moduleOpenDownloadAppTitle.setVisibility(isMainAppInstalled() ?
				View.GONE :
				View.VISIBLE);
		binding.moduleOpenDownloadAppButton.setText(isMainAppInstalled() ?
				R.string.action_open_main_app :
				R.string.action_download_main_app);
		binding.moduleOpenDownloadAppButton.setVisibility(isMainAppInstalled() ?
				View.VISIBLE :
				View.GONE);
		binding.getItOnGooglePlay.setVisibility(isMainAppInstalled() ?
				View.GONE :
				View.VISIBLE);
	}

	private boolean isMainAppInstalled() {
		return PackageManagerUtils.isAppInstalled(this, Constants.MAIN_APP_PACKAGE_NAME);
	}

	private void onCountDownTimeStateChanged(long millisUntilFinished) {
		if (!FeatureFlags.F_MODULE_AUTO_OPEN) return;
		if (this.binding != null) {
			final int seconds = Math.round(millisUntilFinished / 1000.0f);
			binding.moduleCountdownText.setText(
					getResources().getQuantityString(R.plurals.text_opening_main_app_in_and_seconds, seconds, seconds)
			);
		}
	}

	private void onCountDownCancelStateChanged() {
		if (!FeatureFlags.F_MODULE_AUTO_OPEN) return;
		if (this.binding == null) return;
		if (this.countDownCancelled) {
			binding.moduleCountdownText.setVisibility(View.INVISIBLE);
			binding.moduleCountdownCancelText.setVisibility(View.INVISIBLE);
		} else {
			binding.moduleCountdownText.setVisibility(View.VISIBLE);
			binding.moduleCountdownCancelText.setVisibility(View.VISIBLE);
		}
	}

	@MainThread
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
			StoreUtils.viewAppPage(this,
					Constants.MAIN_APP_PACKAGE_NAME_RELEASE, // always release flavor for Play Store
					LinkUtils.NO_LABEL,
					PackageManagerExtKt.toSimplePkg(getPackageName()), "mt-module", null, null, null);
		}
		finish();
	}

	private void openMainApp() {
		PackageManagerUtils.openApp(
				this,
				Constants.MAIN_APP_PACKAGE_NAME,
				Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK
		);
	}

	private static final String PREFS_KEEP_MODULE_APP_LAUNCHER_ICON = "pKeepModuleAppLauncherIcon";

	@MainThread
	private void checkKeepTempIcon() {
		final CheckBox keepTempIconCb = findViewById(R.id.module_keep_temp_icon);
		final boolean keepTempIcon = keepTempIconCb.isChecked();
		final Context appContext = this.getApplicationContext();
		TaskUtils.THREAD_POOL_EXECUTOR.execute(() ->
				PreferenceUtils.getPrefLcl(appContext).edit()
						.putBoolean(PREFS_KEEP_MODULE_APP_LAUNCHER_ICON, keepTempIcon)
						.apply()
		);
		if (keepTempIcon) {
			PackageManagerUtils.resetLauncherIcon(this);
		} else {
			PackageManagerUtils.removeLauncherIcon(this);
		}
	}
}

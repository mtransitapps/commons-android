package org.mtransit.android.commons.provider.common;

import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.MTLog;

import java.util.concurrent.TimeUnit;

public interface ProviderContract extends MTLog.Loggable {

	String PING_PATH = "ping";

	long MAX_CACHE_VALIDITY_MS = TimeUnit.DAYS.toMillis(1_000L);

	@NonNull
	UriMatcher getURI_MATCHER();

	@WorkerThread
	@NonNull
	SQLiteDatabase getReadDB();

	@WorkerThread
	@NonNull
	SQLiteDatabase getWriteDB();

	@NonNull
	Context requireContextCompat();

	abstract class Filter {

		private static final boolean CACHE_ONLY_DEFAULT = false;
		private static final boolean ASYNC_ONLY_DEFAULT = false;
		private static final boolean IN_FOCUS_DEFAULT = false;

		@Nullable
		private Boolean cacheOnly = null;
		@Nullable
		private Boolean asyncOnly = null;
		@Nullable
		private Boolean inFocus = null;

		public boolean isCacheOnlyOrDefault() {
			return this.cacheOnly == null ? CACHE_ONLY_DEFAULT : this.cacheOnly;
		}

		@Nullable
		public Boolean getCacheOnlyOrNull() {
			return this.cacheOnly;
		}

		@NonNull
		public void setCacheOnly(@Nullable Boolean cacheOnly) {
			this.cacheOnly = cacheOnly;
		}

		public void setAsyncOnly(@Nullable Boolean asyncOnly) {
			this.asyncOnly = asyncOnly;
		}

		public boolean isAsyncOnlyOrDefault() {
			return this.asyncOnly == null ? ASYNC_ONLY_DEFAULT : this.asyncOnly;
		}

		@Nullable
		public Boolean getAsyncOnlyOrNull() {
			return this.asyncOnly;
		}

		public boolean isInFocusOrDefault() {
			return this.inFocus == null ? IN_FOCUS_DEFAULT : this.inFocus;
		}

		@Nullable
		public Boolean getInFocusOrNull() {
			return this.inFocus;
		}

		public void setInFocus(@Nullable Boolean inFocus) {
			this.inFocus = inFocus;
		}

		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_ASYNC_ONLY = "asyncOnly";
		private static final String JSON_IN_FOCUS = "inFocus";

		public static void toJSON(@NonNull Filter filter, @NonNull JSONObject json) throws JSONException {
			if (filter.getCacheOnlyOrNull() != null) {
				json.put(JSON_CACHE_ONLY, filter.getCacheOnlyOrNull());
			}
			if (filter.getAsyncOnlyOrNull() != null) {
				json.put(JSON_ASYNC_ONLY, filter.getAsyncOnlyOrNull());
			}
			if (filter.getInFocusOrNull() != null) {
				json.put(JSON_IN_FOCUS, filter.getInFocusOrNull());
			}
		}

		public static void fromJSON(@NonNull Filter filter, @NonNull JSONObject json) throws JSONException {
			if (json.has(JSON_CACHE_ONLY)) {
				filter.cacheOnly = json.getBoolean(JSON_CACHE_ONLY);
			}
			if (json.has(JSON_ASYNC_ONLY)) {
				filter.asyncOnly = json.getBoolean(JSON_ASYNC_ONLY);
			}
			if (json.has(JSON_IN_FOCUS)) {
				filter.inFocus = json.getBoolean(JSON_IN_FOCUS);
			}
		}

		@NonNull
		public String toStringParts() {
			final StringBuilder sb = new StringBuilder();
			sb.append("cacheOnly:").append(this.cacheOnly).append(',');
			sb.append("asyncOnly:").append(this.asyncOnly).append(',');
			sb.append("inFocus:").append(this.inFocus).append(",");
			return sb.toString();
		}
	}
}


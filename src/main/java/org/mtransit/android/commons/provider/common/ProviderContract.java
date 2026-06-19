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
		private static final boolean IN_FOCUS_DEFAULT = false;

		@Nullable
		private Boolean cacheOnly = null;
		@Nullable
		private Long cacheValidityInMs = null;
		@Nullable
		private Boolean inFocus = null;

		public boolean isCacheOnlyOrDefault() {
			return this.cacheOnly == null ? CACHE_ONLY_DEFAULT : this.cacheOnly;
		}

		@SuppressWarnings("unused")
		@Nullable
		public Boolean getCacheOnlyOrNull() {
			return this.cacheOnly;
		}

		public void setCacheOnly(@Nullable Boolean cacheOnly) {
			this.cacheOnly = cacheOnly;
		}

		@Nullable
		public Long getCacheValidityInMsOrNull() {
			return this.cacheValidityInMs;
		}

		@SuppressWarnings("unused")
		public void setCacheValidityInMs(@Nullable Long cacheValidityInMs) {
			this.cacheValidityInMs = cacheValidityInMs;
		}

		public boolean isInFocusOrDefault() {
			return this.inFocus == null ? IN_FOCUS_DEFAULT : this.inFocus;
		}

		@SuppressWarnings("unused")
		@Nullable
		public Boolean getInFocusOrNull() {
			return this.inFocus;
		}

		public void setInFocus(@Nullable Boolean inFocus) {
			this.inFocus = inFocus;
		}

		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";
		private static final String JSON_IN_FOCUS = "inFocus";

		@Nullable
		@SuppressWarnings("unused")
		public static Long getCacheValidityInMsFromJSON(@NonNull JSONObject json) throws JSONException {
			return json.has(JSON_CACHE_VALIDITY_IN_MS) ? json.getLong(JSON_CACHE_VALIDITY_IN_MS) : null;
		}

		public static void toJSON(@NonNull Filter filter, @NonNull JSONObject json) throws JSONException {
			if (filter.cacheOnly != null) {
				json.put(JSON_CACHE_ONLY, filter.cacheOnly);
			}
			if (filter.cacheValidityInMs != null) {
				json.put(JSON_CACHE_VALIDITY_IN_MS, filter.cacheValidityInMs);
			}
			if (filter.inFocus != null) {
				json.put(JSON_IN_FOCUS, filter.inFocus);
			}
		}

		public static void fromJSON(@NonNull Filter filter, @NonNull JSONObject json) throws JSONException {
			if (json.has(JSON_CACHE_ONLY)) {
				filter.cacheOnly = json.getBoolean(JSON_CACHE_ONLY);
			}
			if (json.has(JSON_CACHE_VALIDITY_IN_MS)) {
				filter.cacheValidityInMs = json.getLong(JSON_CACHE_VALIDITY_IN_MS);
			}
			if (json.has(JSON_IN_FOCUS)) {
				filter.inFocus = json.getBoolean(JSON_IN_FOCUS);
			}
		}

		@NonNull
		protected String toStringParts() {
			final StringBuilder sb = new StringBuilder();
			if (this.cacheOnly != null) sb.append("cacheOnly:").append(this.cacheOnly).append(',');
			if (this.cacheValidityInMs != null) sb.append("cacheValidityInMs:").append(MTLog.formatDuration(this.cacheValidityInMs)).append(',');
			if (this.inFocus != null) sb.append("inFocus:").append(this.inFocus).append(",");
			return sb.toString();
		}
	}
}


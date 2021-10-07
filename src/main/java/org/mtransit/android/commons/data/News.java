package org.mtransit.android.commons.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.NewsProviderContract;

import java.util.Comparator;
import java.util.Objects;

public class News implements MTLog.Loggable {

	private static final String LOG_TAG = News.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final NewsComparator NEWS_COMPARATOR = new NewsComparator();
	public static final NewsSeverityComparator NEWS_SEVERITY_COMPARATOR = new NewsSeverityComparator();

	@Nullable
	private final Integer id; // internal DB ID (useful to delete) OR NULL
	@NonNull
	private final String authority;
	@NonNull
	private final String uuid;
	private final int severity;
	private final long noteworthyInMs;
	private final long lastUpdateInMs;
	private final long maxValidityInMs;
	private final long createdAtInMs;
	@NonNull
	private final String targetUUID;
	@NonNull
	private final String color;
	@NonNull
	private final String authorName;
	@Nullable
	private final String authorUsername;
	@Nullable
	private final String authorPictureURL;
	@NonNull
	private final String authorProfileURL;
	@NonNull
	private final String text;
	@Nullable
	private final String textHTML;
	@NonNull
	private final String webURL;
	@NonNull
	private final String language;
	@NonNull
	private final String sourceId;
	@NonNull
	private final String sourceLabel;

	public News(@Nullable Integer optId,
				@NonNull String authority,
				@NonNull String uuid,
				int severity,
				long noteworthyForInMs,
				long lastUpdateInMs,
				long maxValidityInMs,
				long createdAtInMs,
				@NonNull String targetUUID,
				@NonNull String color,
				@NonNull String authorName,
				@Nullable String authorUsername,
				@Nullable String authorPictureURL,
				@NonNull String authorProfileURL,
				@NonNull String text,
				@Nullable String optTextHTML,
				@NonNull String webURL,
				@NonNull String language,
				@NonNull String sourceId,
				@NonNull String sourceLabel) {
		this.id = optId;
		this.authority = authority;
		this.uuid = uuid;
		this.severity = severity;
		this.noteworthyInMs = noteworthyForInMs;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
		this.createdAtInMs = createdAtInMs;
		this.targetUUID = targetUUID;
		this.color = color;
		this.colorInt = null;
		this.authorName = authorName;
		this.authorUsername = authorUsername;
		this.authorPictureURL = authorPictureURL;
		this.authorProfileURL = authorProfileURL;
		this.text = text;
		this.textHTML = optTextHTML;
		this.webURL = webURL;
		this.language = language;
		this.sourceId = sourceId;
		this.sourceLabel = sourceLabel;
	}

	@NonNull
	@Override
	public String toString() {
		return News.class.getSimpleName() + "{" +
				"id=" + id +
				", uuid='" + uuid + '\'' +
				", targetUUID='" + targetUUID + '\'' +
				", text='" + text + '\'' +
				'}';
	}

	@Nullable
	public Integer getId() {
		return this.id;
	}

	@NonNull
	public String getAuthority() {
		return authority;
	}

	@NonNull
	public String getUUID() {
		return uuid;
	}

	public int getSeverity() {
		return severity;
	}

	public long getNoteworthyInMs() {
		return noteworthyInMs;
	}

	public boolean isUseful() {
		return this.lastUpdateInMs + this.maxValidityInMs >= TimeUtils.currentTimeMillis();
	}

	@NonNull
	public String getAuthorOneLine() {
		if (TextUtils.isEmpty(this.authorUsername)) {
			return this.authorName;
		}
		return this.authorName + " (" + this.authorUsername + ")";
	}

	@Nullable
	public String getAuthorPictureURL() {
		return authorPictureURL;
	}

	public boolean hasAuthorPictureURL() {
		return this.authorPictureURL != null && !this.authorPictureURL.isEmpty();
	}

	@NonNull
	public String getAuthorProfileURL() {
		return authorProfileURL;
	}

	public long getLastUpdateInMs() {
		return lastUpdateInMs;
	}

	public long getCreatedAtInMs() {
		return createdAtInMs;
	}

	@NonNull
	public String getColor() {
		return color;
	}

	public boolean hasColor() {
		return !TextUtils.isEmpty(this.color);
	}

	@Nullable
	@ColorInt
	private Integer colorInt;

	@ColorInt
	public int getColorInt() {
		if (colorInt == null) {
			colorInt = ColorUtils.parseColor(getColor());
		}
		return colorInt;
	}

	@ColorInt
	@Nullable
	public Integer getColorIntOrNull() {
		if (!hasColor()) {
			return null;
		}
		return getColorInt();
	}

	@NonNull
	public String getText() {
		return text;
	}

	@NonNull
	public String getSourceLabel() {
		return sourceLabel;
	}

	@NonNull
	public String getTextHTML() {
		if (textHTML == null || textHTML.isEmpty()) {
			return getText();
		}
		return textHTML;
	}

	@NonNull
	public String getWebURL() {
		return webURL;
	}

	@NonNull
	public static News fromCursorStatic(@NonNull Cursor cursor, @NonNull String authority) {
		int idIdx = cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_ID);
		Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		String uuid = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_UUID));
		int severity = cursor.getInt(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_SEVERITY));
		long noteworthyInMs = cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_NOTEWORTHY));
		long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_LAST_UPDATE));
		long maxValidityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_MAX_VALIDITY_IN_MS));
		long createdAtInMs = cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_CREATED_AT));
		String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID));
		String color = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_COLOR));
		String authorName = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_NAME));
		String authorUsername = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_USERNAME));
		String authorPictureURL = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PICTURE_URL));
		String authorProfileURL = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PROFILE_URL));
		String text = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_TEXT));
		String textHTML = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_TEXT_HTML));
		String webURL = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_WEB_URL));
		String language = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_LANGUAGE));
		String sourceId = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID));
		String sourceLabel = cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_SOURCE_LABEL));
		return new News(id, authority, uuid, severity, noteworthyInMs, lastUpdateInMs, maxValidityInMs, createdAtInMs, targetUUID, color, authorName,
				authorUsername, authorPictureURL, authorProfileURL, text, textHTML, webURL, language, sourceId, sourceLabel);
	}

	@NonNull
	public ContentValues toContentValues() {
		ContentValues contentValues = new ContentValues();
		if (this.id != null) {
			contentValues.put(NewsProviderContract.Columns.T_NEWS_K_ID, this.id);
		} // ELSE AUTO INCREMENT
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_UUID, this.uuid);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_SEVERITY, this.severity);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_NOTEWORTHY, this.noteworthyInMs);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_LAST_UPDATE, this.lastUpdateInMs);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_MAX_VALIDITY_IN_MS, this.maxValidityInMs);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_CREATED_AT, this.createdAtInMs);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID, this.targetUUID);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_COLOR, getColor());
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_NAME, this.authorName);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_USERNAME, this.authorUsername);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PICTURE_URL, this.authorPictureURL);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PROFILE_URL, this.authorProfileURL);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_TEXT, this.text);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_TEXT_HTML, this.textHTML);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_WEB_URL, this.webURL);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_LANGUAGE, this.language);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID, this.sourceId);
		contentValues.put(NewsProviderContract.Columns.T_NEWS_K_SOURCE_LABEL, this.sourceLabel);
		return contentValues;
	}

	/**
	 * {@link NewsProviderContract#PROJECTION_NEWS}
	 */
	@NonNull
	public Object[] getCursorRow() {
		return new Object[]{ //
				id, //
				authority, //
				uuid, //
				severity, //
				noteworthyInMs, //
				lastUpdateInMs,//
				maxValidityInMs, //
				createdAtInMs, //
				targetUUID, //
				getColor(), //
				authorName, //
				authorUsername, //
				authorPictureURL, //
				authorProfileURL, //
				text, //
				textHTML, //
				webURL, //
				language,//
				sourceId, //
				sourceLabel //
		};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		News news = (News) o;

		if (severity != news.severity) return false;
		if (noteworthyInMs != news.noteworthyInMs) return false;
		if (lastUpdateInMs != news.lastUpdateInMs) return false;
		if (maxValidityInMs != news.maxValidityInMs) return false;
		if (createdAtInMs != news.createdAtInMs) return false;
		if (!Objects.equals(id, news.id)) return false;
		if (!authority.equals(news.authority)) return false;
		if (!uuid.equals(news.uuid)) return false;
		if (!targetUUID.equals(news.targetUUID)) return false;
		if (!color.equals(news.color)) return false;
		if (!authorName.equals(news.authorName)) return false;
		if (!Objects.equals(authorUsername, news.authorUsername)) return false;
		if (!Objects.equals(authorPictureURL, news.authorPictureURL)) return false;
		if (!authorProfileURL.equals(news.authorProfileURL)) return false;
		if (!text.equals(news.text)) return false;
		if (!Objects.equals(textHTML, news.textHTML)) return false;
		if (!webURL.equals(news.webURL)) return false;
		if (!language.equals(news.language)) return false;
		if (!sourceId.equals(news.sourceId)) return false;
		return sourceLabel.equals(news.sourceLabel);
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + authority.hashCode();
		result = 31 * result + uuid.hashCode();
		result = 31 * result + severity;
		result = 31 * result + (int) (noteworthyInMs ^ (noteworthyInMs >>> 32));
		result = 31 * result + (int) (lastUpdateInMs ^ (lastUpdateInMs >>> 32));
		result = 31 * result + (int) (maxValidityInMs ^ (maxValidityInMs >>> 32));
		result = 31 * result + (int) (createdAtInMs ^ (createdAtInMs >>> 32));
		result = 31 * result + targetUUID.hashCode();
		result = 31 * result + color.hashCode();
		result = 31 * result + authorName.hashCode();
		result = 31 * result + (authorUsername != null ? authorUsername.hashCode() : 0);
		result = 31 * result + (authorPictureURL != null ? authorPictureURL.hashCode() : 0);
		result = 31 * result + authorProfileURL.hashCode();
		result = 31 * result + text.hashCode();
		result = 31 * result + (textHTML != null ? textHTML.hashCode() : 0);
		result = 31 * result + webURL.hashCode();
		result = 31 * result + language.hashCode();
		result = 31 * result + sourceId.hashCode();
		result = 31 * result + sourceLabel.hashCode();
		return result;
	}

	public static class NewsComparator implements Comparator<News> {
		@Override
		public int compare(@Nullable News lhs, @Nullable News rhs) {
			long lCreatedAtInMs = lhs == null ? 0L : lhs.getCreatedAtInMs();
			long rCreatedAtInMs = rhs == null ? 0L : rhs.getCreatedAtInMs();
			return Long.compare(rCreatedAtInMs, lCreatedAtInMs);
		}
	}

	public static class NewsSeverityComparator implements Comparator<News> {
		@Override
		public int compare(@Nullable News lhs, @Nullable News rhs) {
			int lSeverity = lhs == null ? 0 : lhs.getSeverity();
			int rSeverity = rhs == null ? 0 : rhs.getSeverity();
			if (lSeverity != rSeverity) {
				return rSeverity - lSeverity;
			}
			long lCreatedAtInMs = lhs == null ? 0L : lhs.getCreatedAtInMs();
			long rCreatedAtInMs = rhs == null ? 0L : rhs.getCreatedAtInMs();
			return Long.compare(rCreatedAtInMs, lCreatedAtInMs);
		}
	}
}

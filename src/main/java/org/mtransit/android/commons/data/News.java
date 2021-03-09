package org.mtransit.android.commons.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.NewsProviderContract;

import java.util.Comparator;

public class News implements MTLog.Loggable {

	private static final String LOG_TAG = News.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final NewsComparator NEWS_COMPARATOR = new NewsComparator();
	public static final NewsSeverityComparator NEWS_SEVERITY_COMPARATOR = new NewsSeverityComparator();

	private Integer id; // internal DB ID (useful to delete) OR NULL
	private String authority;
	private String uuid;
	private int severity;
	private long noteworthyInMs;
	private long lastUpdateInMs;
	private long maxValidityInMs;
	private long createdAtInMs;
	private String targetUUID;
	private String color;
	private String authorName;
	private String authorUsername;
	private String authorPictureURL;
	private String authorProfileURL;
	private String text;
	private String textHTML;
	private String webURL;
	private String language;
	private String sourceId;
	private String sourceLabel;

	public News(Integer optId, String authority, String uuid, int severity, long noteworthyForInMs, long lastUpdateInMs, long maxValidityInMs,
			long createdAtInMs, String targetUUID, String color, String authorName, String authorUsername, String authorPictureURL, String authorProfileURL,
			String text, String optTextHTML, String webURL, String language, String sourceId, String sourceLabel) {
		this.id = optId;
		this.authority = authority;
		this.uuid = uuid;
		this.severity = severity;
		this.noteworthyInMs = noteworthyForInMs;
		this.lastUpdateInMs = lastUpdateInMs;
		this.maxValidityInMs = maxValidityInMs;
		this.createdAtInMs = createdAtInMs;
		this.targetUUID = targetUUID;
		setColor(color);
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
		return new StringBuilder(ServiceUpdate.class.getSimpleName()).append('[') //
				.append("id:").append(this.id) //
				.append(',') //
				.append("uuid:").append(this.uuid) //
				.append(',') //
				.append("targetUUID:").append(this.targetUUID) //
				.append(',') //
				.append("text:").append(this.text) //
				.append(']').toString();
	}

	public Integer getId() {
		return this.id;
	}

	public String getAuthority() {
		return authority;
	}

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

	public String getAuthorOneLine() {
		if (TextUtils.isEmpty(this.authorUsername)) {
			return this.authorName;
		}
		return this.authorName + " (" + this.authorUsername + ")";
	}

	public String getAuthorProfileURL() {
		return authorProfileURL;
	}

	public long getLastUpdateInMs() {
		return lastUpdateInMs;
	}

	public long getCreatedAtInMs() {
		return createdAtInMs;
	}

	public String getColor() {
		return color;
	}

	public boolean hasColor() {
		return !TextUtils.isEmpty(this.color);
	}

	private void setColor(String color) {
		this.color = color;
		this.colorInt = null;
	}

	@ColorInt
	private Integer colorInt = null;

	@ColorInt
	public int getColorInt() {
		if (colorInt == null) {
			colorInt = ColorUtils.parseColor(getColor());
		}
		return colorInt;
	}

	public String getText() {
		return text;
	}

	public String getSourceLabel() {
		return sourceLabel;
	}

	public String getTextHTML() {
		if (TextUtils.isEmpty(textHTML)) {
			return getText();
		}
		return textHTML;
	}

	public String getWebURL() {
		return webURL;
	}

	public static News fromCursorStatic(Cursor cursor, String authority) {
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
	public Object[] getCursorRow() {
		return new Object[] { //
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

	private static class NewsComparator implements Comparator<News> {
		@Override
		public int compare(News lhs, News rhs) {
			long lCreatedAtInMs = lhs == null ? 0L : lhs.getCreatedAtInMs();
			long rCreatedAtInMs = rhs == null ? 0L : rhs.getCreatedAtInMs();
			return Long.compare(rCreatedAtInMs, lCreatedAtInMs);
		}
	}

	private static class NewsSeverityComparator implements Comparator<News> {
		@Override
		public int compare(News lhs, News rhs) {
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

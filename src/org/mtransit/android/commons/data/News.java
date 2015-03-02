package org.mtransit.android.commons.data;

import java.util.Comparator;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.ComparatorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.NewsProvider;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

public class News implements MTLog.Loggable {

	private static final String TAG = News.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final NewsComparator NEWS_COMPARATOR = new NewsComparator();

	private Integer id; // internal DB ID (useful to delete) OR NULL
	private String authority;
	private String uuid;
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

	public News(Integer optId, String authority, String uuid, long lastUpdateInMs, long maxValidityInMs, long createdAtInMs, String targetUUID, String color,
			String authorName, String authorUsername, String authorPictureURL, String authorProfileURL, String text, String optTextHTML, String webURL,
			String language, String sourceId, String sourceLabel) {
		this.id = optId;
		this.authority = authority;
		this.uuid = uuid;
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

	private Integer colorInt = null;

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
		int idIdx = cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_ID);
		Integer id = cursor.isNull(idIdx) ? null : cursor.getInt(idIdx);
		String uuid = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_UUID));
		long lastUpdateInMs = cursor.getLong(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_LAST_UPDATE));
		long maxValidityInMs = cursor.getLong(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_MAX_VALIDITY_IN_MS));
		long createdAtInMs = cursor.getLong(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_CREATED_AT));
		String targetUUID = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_TARGET_UUID));
		String color = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_COLOR));
		String authorName = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_NAME));
		String authorUsername = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_USERNAME));
		String authorPictureURL = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_PICTURE_URL));
		String authorProfileURL = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_PROFILE_URL));
		String text = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_TEXT));
		String textHTML = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_TEXT_HTML));
		String webURL = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_WEB_URL));
		String language = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_LANGUAGE));
		String sourceId = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_SOURCE_ID));
		String sourceLabel = cursor.getString(cursor.getColumnIndexOrThrow(NewsProvider.NewsColumns.T_NEWS_K_SOURCE_LABEL));
		return new News(id, authority, uuid, lastUpdateInMs, maxValidityInMs, createdAtInMs, targetUUID, color, authorName, authorUsername, authorPictureURL,
				authorProfileURL, text, textHTML, webURL, language, sourceId, sourceLabel);
	}

	public ContentValues toContentValues() {
		ContentValues contentValues = new ContentValues();
		if (this.id != null) {
			contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_ID, this.id);
		} // ELSE AUTO INCREMENT
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_UUID, this.uuid);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_LAST_UPDATE, this.lastUpdateInMs);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_MAX_VALIDITY_IN_MS, this.maxValidityInMs);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_CREATED_AT, this.createdAtInMs);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_TARGET_UUID, this.targetUUID);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_COLOR, getColor());
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_NAME, this.authorName);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_USERNAME, this.authorUsername);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_PICTURE_URL, this.authorPictureURL);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_AUTHOR_PROFILE_URL, this.authorProfileURL);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_TEXT, this.text);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_TEXT_HTML, this.textHTML);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_WEB_URL, this.webURL);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_LANGUAGE, this.language);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_SOURCE_ID, this.sourceId);
		contentValues.put(NewsProvider.NewsColumns.T_NEWS_K_SOURCE_LABEL, this.sourceLabel);
		return contentValues;
	}

	/**
	 * {@link NewsProvider#PROJECTION_NEWS}
	 */
	public Object[] getCursorRow() {
		return new Object[] { //
		id, //
				authority, //
				uuid, //
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
			long lCreatedAtInMs = lhs == null ? 0l : lhs.getCreatedAtInMs();
			long rCreatedAtInMs = rhs == null ? 0l : rhs.getCreatedAtInMs();
			if (lCreatedAtInMs > rCreatedAtInMs) {
				return ComparatorUtils.BEFORE;
			} else if (rCreatedAtInMs > lCreatedAtInMs) {
				return ComparatorUtils.AFTER;
			} else {
				return ComparatorUtils.SAME;
			}
		}
	}
}

package org.mtransit.android.commons.provider;

import android.content.Context;
import android.support.annotation.NonNull;

public abstract class ContentProviderExtra extends MTContentProvider {

	@NonNull
	public final Context requireContext() {
		Context context = this.getContext();
		if (context == null) {
			throw new IllegalStateException("ContentProvider " + this + " not attached to a context.");
		} else {
			return context;
		}
	}
}

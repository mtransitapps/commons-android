package org.mtransit.android.commons.api;


import org.mtransit.android.commons.MTLog;

import android.view.ViewTreeObserver;
public interface SupportUtil extends MTLog.Loggable {


	void removeOnGlobalLayoutListener(ViewTreeObserver viewTreeObserver, ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener);
}

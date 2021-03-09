package org.mtransit.android.commons.ui.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTSurfaceView extends SurfaceView implements MTLog.Loggable {

	public MTSurfaceView(Context context) {
		super(context);
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "%s(%s)", getLogTag(), context);
		}
	}

	public MTSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s)", getLogTag(), context, attrs);
		}
	}

	public MTSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s, %s)", getLogTag(), context, attrs, defStyle);
		}
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "dispatchDraw(%s)", canvas);
		}
		super.dispatchDraw(canvas);
	}

	@Override
	public boolean gatherTransparentRegion(Region region) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "gatherTransparentRegion(%s)", region);
		}
		return super.gatherTransparentRegion(region);
	}

	@Override
	public SurfaceHolder getHolder() {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "getHolder()");
		}
		return super.getHolder();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void setSecure(boolean isSecure) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "setSecure(%s)", isSecure);
		}
		super.setSecure(isSecure);
	}

	@Override
	public void setVisibility(int visibility) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "setVisibility(%s)", visibility);
		}
		super.setVisibility(visibility);
	}

	@Override
	public void setZOrderMediaOverlay(boolean isMediaOverlay) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "setZOrderMediaOverlay(%s)", isMediaOverlay);
		}
		super.setZOrderMediaOverlay(isMediaOverlay);
	}

	@Override
	public void setZOrderOnTop(boolean onTop) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "setZOrderOnTop(%s)", onTop);
		}
		super.setZOrderOnTop(onTop);
	}

	// INHERITED FROM VIEW

	@Override
	public void buildDrawingCache(boolean autoScale) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "buildDrawingCache(%s)", autoScale);
		}
		super.buildDrawingCache(autoScale);
	}

	@Override
	public void destroyDrawingCache() {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "destroyDrawingCache()");
		}
		super.destroyDrawingCache();
	}

	@Override
	public void draw(Canvas canvas) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "draw(%s)", canvas);
		}
		super.draw(canvas);
	}

	@Override
	public void invalidate() {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "invalidate()");
		}
		super.invalidate();
	}

	@Override
	public void invalidate(int l, int t, int r, int b) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "invalidate(%s,%s,%s,%s)", l, t, r, b);
		}
		super.invalidate(l, t, r, b);
	}

	@Override
	public void invalidate(Rect dirty) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "invalidate(%s)", dirty);
		}
		super.invalidate(dirty);
	}

	@Override
	public void invalidateDrawable(Drawable drawable) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "invalidateDrawable(%s)", drawable);
		}
		super.invalidateDrawable(drawable);
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onConfigurationChanged(%s)", newConfig);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onAttachedToWindow() {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onAttachedToWindow()");
		}
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onDetachedFromWindow()");
		}
		super.onDetachedFromWindow();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onDraw(%s)", canvas);
		}
		super.onDraw(canvas);
	}

	@Override
	protected void onFinishInflate() {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onFinishInflate()");
		}
		super.onFinishInflate();
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onFocusChanged(%s,%s,%s)", gainFocus, direction, previouslyFocusedRect);
		}
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onKeyDown(%s,%s)", keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onKeyUp(%s,%s)", keyCode, event);
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onLayout(%s,%s,%s,%s,%s)", changed, left, top, right, bottom);
		}
		super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onMeasure(%s,%s)", widthMeasureSpec, heightMeasureSpec);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onSizeChanged(%s,%s,%s,%s)", w, h, oldw, oldh);
		}
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onTrackballEvent(%s)", event);
		}
		return super.onTrackballEvent(event);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onWindowFocusChanged(%s)", hasWindowFocus);
		}
		super.onWindowFocusChanged(hasWindowFocus);
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "onWindowVisibilityChanged(%s)", visibility);
		}
		super.onWindowVisibilityChanged(visibility);
	}

}

package org.mtransit.android.commons.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MTSuperscriptImageSpan extends ImageSpan {

	@NonNull
	private final Drawable drawable;

	public MTSuperscriptImageSpan(@NonNull Drawable drawable, int verticalAlignment) {
		super(drawable, verticalAlignment);
		this.drawable = drawable;
	}

	@Override
	public int getSize(@NonNull Paint paint, @Nullable CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
		return super.getSize(paint, text, start, end, fm);
	}

	@Override
	public void draw(@NonNull Canvas canvas, @Nullable CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
		Drawable b = this.drawable;
		canvas.save();
		int transY = bottom - b.getBounds().bottom;
		transY = (bottom - top) / 2 - b.getBounds().height() / 2;
		transY = (bottom - top) / 8 - b.getBounds().height() / 8;
		canvas.translate(x, transY);
		b.draw(canvas);
		canvas.restore();
	}
}

package org.mtransit.android.commons.ui.widget;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class MTBaseAdapter extends BaseAdapter implements MTLog.Loggable {

	@Override
	public int getCount() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getCount()");
		}
		return getCountMT();
	}

	public abstract int getCountMT();

	@Override
	public Object getItem(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getItem(%s)", position);
		}
		return getItemMT(position);
	}

	public abstract Object getItemMT(int position);

	@Override
	public long getItemId(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getItemId(%s)", position);
		}
		return getItemIdMT(position);
	}

	public abstract long getItemIdMT(int position);

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getView(%s,%s,%s)", position, convertView, parent);
		}
		return getViewMT(position, convertView, parent);
	}

	public abstract View getViewMT(int position, View convertView, ViewGroup parent);

	// INHERIRED FROM LIST ADAPTER

	@Override
	public boolean areAllItemsEnabled() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "areAllItemsEnabled()");
		}
		return super.areAllItemsEnabled();
	}

	@Override
	public boolean isEnabled(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "isEnabled(%s)", position);
		}
		return super.isEnabled(position);
	}

	// INHERITED FROM ADAPTER

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "registerDataSetObserver(%s)", observer);
		}
		super.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "registerDataSetObserver(%s)", observer);
		}
		super.unregisterDataSetObserver(observer);
	}

	@Override
	public boolean hasStableIds() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "hasStableIds()");
		}
		return super.hasStableIds();
	}

	@Override
	public int getItemViewType(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getItemViewType(%s)", position);
		}
		return super.getItemViewType(position);
	}

	@Override
	public int getViewTypeCount() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getViewTypeCount()");
		}
		return super.getViewTypeCount();
	}

	@Override
	public boolean isEmpty() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "isEmpty()");
		}
		return super.isEmpty();
	}
}

package org.mtransit.android.commons.ui.widget;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTAdapter implements Adapter, MTLog.Loggable {

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "registerDataSetObserver(%s)", observer);
		}
		registerDataSetObserverMT(observer);
	}

	public abstract void registerDataSetObserverMT(DataSetObserver observer);

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "registerDataSetObserver(%s)", observer);
		}
		unregisterDataSetObserverMT(observer);
	}

	public abstract void unregisterDataSetObserverMT(DataSetObserver observer);

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
	public boolean hasStableIds() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "hasStableIds()");
		}
		return hasStableIdsMT();
	}

	public abstract boolean hasStableIdsMT();

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getView(%s,%s,%s)", position, convertView, parent);
		}
		return getViewMT(position, convertView, parent);
	}

	public abstract View getViewMT(int position, View convertView, ViewGroup parent);

	@Override
	public int getItemViewType(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getItemViewType(%s)", position);
		}
		return getItemViewTypeMT(position);
	}

	public abstract int getItemViewTypeMT(int position);

	@Override
	public int getViewTypeCount() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getViewTypeCount()");
		}
		return getViewTypeCountMT();
	}

	public abstract int getViewTypeCountMT();

	@Override
	public boolean isEmpty() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "isEmpty()");
		}
		return isEmptyMT();
	}

	public abstract boolean isEmptyMT();

}

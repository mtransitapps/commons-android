package org.mtransit.android.commons.ui.widget;

import java.util.Collection;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTArrayAdapter<T> extends ArrayAdapter<T> implements MTLog.Loggable {

	public MTArrayAdapter(Context context, int resource) {
		super(context, resource);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s)", getLogTag(), context, resource);
		}
	}

	public MTArrayAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s)", getLogTag(), context, resource, textViewResourceId);
		}
	}

	public MTArrayAdapter(Context context, int resource, T[] objects) {
		super(context, resource, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s)", getLogTag(), context, resource, objects);
		}
	}

	public MTArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
		super(context, resource, textViewResourceId, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s,%s)", getLogTag(), context, resource, textViewResourceId, objects);
		}
	}

	public MTArrayAdapter(Context context, int resource, java.util.List<T> objects) {
		super(context, resource, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s)", getLogTag(), context, resource, objects);
		}
	}

	public MTArrayAdapter(Context context, int resource, int textViewResourceId, java.util.List<T> objects) {
		super(context, resource, textViewResourceId, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s,%s)", getLogTag(), context, resource, textViewResourceId, objects);
		}
	}

	@Override
	public void add(T object) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "add(%s)", object);
		}
		super.add(object);
	}

	@Override
	public void addAll(Collection<? extends T> collection) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "addAll(%s)", collection);
		}
		super.addAll(collection);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addAll(T... items) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "addAll(%s)", items);
		}
		super.addAll(items);
	}

	@Override
	public void clear() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "clear()");
		}
		super.clear();
	}

	@Override
	public int getCount() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getCount()");
		}
		return super.getCount();
	}

	@Override
	public T getItem(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getItem(%s)", position);
		}
		return super.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getItemId(%s)", position);
		}
		return super.getItemId(position);
	}

	@Override
	public int getViewTypeCount() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getViewTypeCount()");
		}
		return super.getViewTypeCount();
	}

	@Override
	public int getItemViewType(int position) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getItemViewType(%s)", position);
		}
		return super.getItemViewType(position);
	}

	@Override
	public int getPosition(T item) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getPosition(%s)", item);
		}
		return super.getPosition(item);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getDropDownView(%s,%s,%s)", position, convertView, parent);
		}
		return super.getDropDownView(position, convertView, parent);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getView(%s,%s,%s)", position, convertView, parent);
		}
		return super.getView(position, convertView, parent);
	}

	@Override
	public void insert(T object, int index) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "insert(%s,%s)", object, index);
		}
		super.insert(object, index);
	}

	@Override
	public void notifyDataSetChanged() {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "notifyDataSetChanged()");
		}
		super.notifyDataSetChanged();
	}

	@Override
	public void remove(T object) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "remove(%s)", object);
		}
		super.remove(object);
	}

}

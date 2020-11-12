package org.mtransit.android.commons.ui.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import java.util.Collection;

/**
 * NO LOGIC HERE, just logs.
 */
@SuppressWarnings("unused")
public abstract class MTArrayAdapter<T> extends ArrayAdapter<T> implements MTLog.Loggable {

	public MTArrayAdapter(@NonNull Context context, @LayoutRes int resource) {
		super(context, resource);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s)", getLogTag(), context, resource);
		}
	}

	public MTArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId) {
		super(context, resource, textViewResourceId);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s)", getLogTag(), context, resource, textViewResourceId);
		}
	}

	public MTArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull T[] objects) {
		super(context, resource, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s)", getLogTag(), context, resource, objects);
		}
	}

	public MTArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull T[] objects) {
		super(context, resource, textViewResourceId, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s,%s)", getLogTag(), context, resource, textViewResourceId, objects);
		}
	}

	public MTArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull java.util.List<T> objects) {
		super(context, resource, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s)", getLogTag(), context, resource, objects);
		}
	}

	public MTArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull java.util.List<T> objects) {
		super(context, resource, textViewResourceId, objects);
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "%s(%s,%s,%s,%s)", getLogTag(), context, resource, textViewResourceId, objects);
		}
	}

	@Override
	public void add(@Nullable T object) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "add(%s)", object);
		}
		super.add(object);
	}

	@Override
	public void addAll(@NonNull Collection<? extends T> collection) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "addAll(%s)", collection);
		}
		super.addAll(collection);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addAll(@Nullable T... items) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "addAll(%s)", items == null ? null : java.util.Arrays.asList(items));
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

	@Nullable
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
	public int getPosition(@Nullable T item) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getPosition(%s)", item);
		}
		return super.getPosition(item);
	}

	@NonNull
	@Override
	public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getDropDownView(%s,%s,%s)", position, convertView, parent);
		}
		return super.getDropDownView(position, convertView, parent);
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "getView(%s,%s,%s)", position, convertView, parent);
		}
		return super.getView(position, convertView, parent);
	}

	@Override
	public void insert(@Nullable T object, int index) {
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
	public void remove(@Nullable T object) {
		if (Constants.LOG_ADAPTER_LIFECYCLE) {
			MTLog.v(this, "remove(%s)", object);
		}
		super.remove(object);
	}

}

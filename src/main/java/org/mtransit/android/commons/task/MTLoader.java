package org.mtransit.android.commons.task;

import android.content.Context;
import android.content.Loader;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class MTLoader<D> extends Loader<D> implements MTLog.Loggable {

	public MTLoader(@NonNull Context context) {
		super(context);
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@Override
	public void abandon() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "abandon()");
		}
		super.abandon();
	}

	@Override
	public boolean cancelLoad() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "cancelLoad()");
		}
		return super.cancelLoad();
	}

	@Override
	public void commitContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "commitContentChanged()");
		}
		super.commitContentChanged();
	}

	@Override
	public String dataToString(D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "dataToString(%s)", data);
		}
		return super.dataToString(data);
	}

	@Override
	public void deliverCancellation() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "deliverCancellation()");
		}
		super.deliverCancellation();
	}

	@Override
	public void deliverResult(D data) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "deliverResult(%s)", data);
		}
		super.deliverResult(data);
	}

	@Override
	public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "dump(%s,%s,%s,%s)", prefix, fd, writer, args);
		}
		super.dump(prefix, fd, writer, args);
	}

	@Override
	public void forceLoad() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "forceLoad()");
		}
		super.forceLoad();
	}

	@Override
	public boolean isAbandoned() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isAbandoned()");
		}
		return super.isAbandoned();
	}

	@Override
	public boolean isReset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isReset()");
		}
		return super.isReset();
	}

	@Override
	public boolean isStarted() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "isStarted()");
		}
		return super.isStarted();
	}

	@Override
	public void onContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "onContentChanged()");
		}
		super.onContentChanged();
	}

	@Override
	public void registerListener(int id, OnLoadCompleteListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "registerListener(%s,%s)", id, listener);
		}
		super.registerListener(id, listener);
	}

	@Override
	public void registerOnLoadCanceledListener(OnLoadCanceledListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "registerOnLoadCanceledListener(%s)", listener);
		}
		super.registerOnLoadCanceledListener(listener);
	}

	@Override
	public void reset() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "reset()");
		}
		super.reset();
	}

	@Override
	public void rollbackContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "rollbackContentChanged()");
		}
		super.rollbackContentChanged();
	}

	@Override
	public void stopLoading() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "stopLoading()");
		}
		super.stopLoading();
	}

	@Override
	public boolean takeContentChanged() {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "takeContentChanged()");
		}
		return super.takeContentChanged();
	}

	@Override
	public void unregisterListener(OnLoadCompleteListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "unregisterListener(%s)", listener);
		}
		super.unregisterListener(listener);
	}

	@Override
	public void unregisterOnLoadCanceledListener(OnLoadCanceledListener<D> listener) {
		if (Constants.LOG_TASK_LIFECYCLE) {
			MTLog.v(this, "unregisterOnLoadCanceledListener(%s)", listener);
		}
		super.unregisterOnLoadCanceledListener(listener);
	}
}

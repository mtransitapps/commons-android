package org.mtransit.android.commons;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.AbsListView.OnScrollListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

public final class SensorUtils implements MTLog.Loggable {

	private static final String TAG = SensorUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int COMPASS_DEGREE_UPDATE_THRESHOLD = 10; // 10°

	private static final long COMPASS_UPDATE_THRESHOLD_IN_MS = TimeUnit.MILLISECONDS.toMillis(250L);

	private SensorUtils() {
	}

	public static void registerCompassListener(@NonNull Context context, @NonNull SensorEventListener listener) {
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager == null) {
			return;
		}
		mSensorManager.registerListener(listener, getAccelerometerSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(listener, getMagneticFieldSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
	}

	private static Sensor getMagneticFieldSensor(@NonNull SensorManager mSensorManager) {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	private static Sensor getAccelerometerSensor(@NonNull SensorManager mSensorManager) {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	public static float getCompassRotationInDegree(double startLatitude, double startLongitude, double endLatitude, double endLongitude, float orientation,
												   float declination) {
		return LocationUtils.bearTo(startLatitude, startLongitude, endLatitude, endLongitude) - (orientation + declination);
	}

	private static Float calculateOrientation(Context context, float[] accelerometerValues, float[] magneticFieldValues) {
		if (accelerometerValues == null || accelerometerValues.length != 3 || magneticFieldValues == null || magneticFieldValues.length != 3) {
			return null;
		}
		float[] R = new float[9];
		boolean success = SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
		if (!success) {
			return null;
		}
		int[] axis = new int[2];
		axis[0] = SensorManager.AXIS_X;
		axis[1] = SensorManager.AXIS_Y;
		int rotation = getSurfaceRotation(context);
		switch (rotation) {
		case Surface.ROTATION_0:
			break;
		case Surface.ROTATION_90:
			axis[0] = SensorManager.AXIS_Y;
			axis[1] = SensorManager.AXIS_MINUS_X;
			break;
		case Surface.ROTATION_180:
			axis[1] = SensorManager.AXIS_MINUS_Y;
			break;
		case Surface.ROTATION_270:
			axis[0] = SensorManager.AXIS_MINUS_Y;
			axis[1] = SensorManager.AXIS_X;
			break;
		}
		float[] outR = new float[9];
		if (!SensorManager.remapCoordinateSystem(R, axis[0], axis[1], outR)) {
			return null;
		}
		float[] values = new float[3];
		SensorManager.getOrientation(outR, values);
		values[0] = (float) Math.toDegrees(values[0]);
		values[1] = (float) Math.toDegrees(values[1]);
		values[2] = (float) Math.toDegrees(values[2]);
		return values[0];
	}

	private static int getSurfaceRotation(@Nullable Context context) {
		if (context == null) {
			return Surface.ROTATION_0;
		}
		try {
			WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			if (windowManager == null) {
				return Surface.ROTATION_0;
			}
			return windowManager.getDefaultDisplay().getRotation();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while retrieving screen surface rotation!");
			return Surface.ROTATION_0;
		}
	}

	public static void checkForCompass(@NonNull Context context,
									   @NonNull SensorEvent event,
									   @NonNull float[] accelerometerValues,
									   @NonNull float[] magneticFieldValues,
									   @NonNull CompassListener listener) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.length);
			if (magneticFieldValues.length == 3
					&& magneticFieldValues[0] != 0.0f
					&& magneticFieldValues[1] != 0.0f
					&& magneticFieldValues[2] != 0.0f) {
				Float orientation = calculateOrientation(context, accelerometerValues, magneticFieldValues);
				if (orientation != null) {
					listener.updateCompass(orientation, false);
				}
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			System.arraycopy(event.values, 0, magneticFieldValues, 0, event.values.length);
			if (accelerometerValues.length == 3
					&& accelerometerValues[0] != 0.0f
					&& accelerometerValues[1] != 0.0f
					&& accelerometerValues[2] != 0.0f) {
				Float orientation = calculateOrientation(context, accelerometerValues, magneticFieldValues);
				if (orientation != null) {
					listener.updateCompass(orientation, false);
				}
			}
			break;
		default:
			break;
		}
	}

	public static void unregisterSensorListener(@NonNull Context context, @NonNull SensorEventListener listener) {
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager == null) {
			return;
		}
		mSensorManager.unregisterListener(listener);
	}

	public static int convertToPositive360Degree(int degree) {
		while (degree < 0) {
			degree += 360;
		}
		while (degree > 360) {
			degree -= 360;
		}
		return degree;
	}

	public static void updateCompass(boolean force,
									 @Nullable Location currentLocation,
									 int orientation,
									 long now,
									 int scrollState,
									 long lastCompassChanged,
									 int lastCompassInDegree,
									 long minThresholdInMs,
									 @NonNull SensorTaskCompleted callback) {
		if (currentLocation == null || orientation < 0) {
			callback.onSensorTaskCompleted(false, orientation, now);
			return;
		}
		if (!force) {
			if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				callback.onSensorTaskCompleted(false, orientation, now);
				return;
			}
			long diffInMs = now - lastCompassChanged;
			boolean tooSoon = diffInMs <= Math.max(minThresholdInMs, COMPASS_UPDATE_THRESHOLD_IN_MS);
			if (tooSoon) {
				callback.onSensorTaskCompleted(false, orientation, now);
				return;
			}
			float diffInDegree = Math.abs(lastCompassInDegree - orientation);
			boolean notDifferentEnough = diffInDegree <= COMPASS_DEGREE_UPDATE_THRESHOLD;
			if (notDifferentEnough) {
				callback.onSensorTaskCompleted(false, orientation, now);
				return;
			}
		}
		callback.onSensorTaskCompleted(true, orientation, now);
	}

	public static float getLocationDeclination(@NonNull Location location) {
		return new GeomagneticField(
				(float) location.getLatitude(),
				(float) location.getLongitude(),
				(float) location.getAltitude(),
				location.getTime()
		).getDeclination();
	}

	public interface CompassListener {
		void updateCompass(float orientation, boolean force);
	}

	public interface SensorTaskCompleted {
		void onSensorTaskCompleted(boolean result, int orientation, long now);
	}
}

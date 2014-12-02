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

public final class SensorUtils implements MTLog.Loggable {

	private static final String TAG = SensorUtils.class.getSimpleName();
	
	@Override
	public String getLogTag() {
		return TAG;
	}



	public static final int COMPASS_DEGREE_UPDATE_THRESOLD = 10; // 10°
	public static final int COMPASS_UPDATE_THRESOLD_IN_MS = 250; // 0.250 seconds

	private SensorUtils() {
	}

	public static void registerCompassListener(Context context, SensorEventListener listener) {
		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(listener, getAccelerometerSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(listener, getMagneticFieldSensor(mSensorManager), SensorManager.SENSOR_DELAY_UI);
	}

	public static Sensor getMagneticFieldSensor(SensorManager mSensorManager) {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	public static Sensor getAccelerometerSensor(SensorManager mSensorManager) {
		return mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	public static float getCompassRotationInDegree(double startLatitude, double startLongitude, double endLatitude, double endLongitude, float orientation,
			float declination) {
		return LocationUtils.bearTo(startLatitude, startLongitude, endLatitude, endLongitude) - (orientation + declination);
	}


	public static float calculateOrientation(Context context, float[] accelerometerValues, float[] magneticFieldValues) {
		if (accelerometerValues == null || accelerometerValues.length != 3 || magneticFieldValues == null || magneticFieldValues.length != 3) {
			return 0;
		}
		float[] R = new float[9];
		if (!SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues)) {
			return 0;
		}
		int x_axis = SensorManager.AXIS_X;
		int y_axis = SensorManager.AXIS_Y;
		int rotation = getSurfaceRotation(context);
		switch (rotation) {
		case Surface.ROTATION_0:
			break;
		case Surface.ROTATION_90:
			x_axis = SensorManager.AXIS_Y;
			y_axis = SensorManager.AXIS_MINUS_X;
			break;
		case Surface.ROTATION_180:
			y_axis = SensorManager.AXIS_MINUS_Y;
			break;
		case Surface.ROTATION_270:
			x_axis = SensorManager.AXIS_MINUS_Y;
			y_axis = SensorManager.AXIS_X;
			break;
		}
		float[] outR = new float[9];
		if (!SensorManager.remapCoordinateSystem(R, x_axis, y_axis, outR)) {
			return 0;
		}
		float[] values = new float[3];
		SensorManager.getOrientation(outR, values);
		values[0] = (float) Math.toDegrees(values[0]);
		values[1] = (float) Math.toDegrees(values[1]);
		values[2] = (float) Math.toDegrees(values[2]);
		return values[0];
	}

	private static int getSurfaceRotation(Context context) {
		if (context == null) {
			return Surface.ROTATION_0;
		}
		try {
			WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			return windowManager.getDefaultDisplay().getRotation();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while retreiving screen surface rotation!");
			return Surface.ROTATION_0;
		}
	}

	public static void checkForCompass(Context context, SensorEvent event, float[] accelerometerValues, float[] magneticFieldValues, CompassListener listener) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			for (int i = 0; i < event.values.length; i++) {
				accelerometerValues[i] = event.values[i];
			}
			if (magneticFieldValues != null && magneticFieldValues[0] != 0.0f && magneticFieldValues[1] != 0.0f && magneticFieldValues[2] != 0.0f) {
				listener.updateCompass(calculateOrientation(context, accelerometerValues, magneticFieldValues), false);
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			for (int i = 0; i < event.values.length; i++) {
				magneticFieldValues[i] = event.values[i];
			}
			if (accelerometerValues != null && accelerometerValues[0] != 0.0f && accelerometerValues[1] != 0.0f && accelerometerValues[2] != 0.0f) {
				listener.updateCompass(calculateOrientation(context, accelerometerValues, magneticFieldValues), false);
			}
			break;
		default:
			break;
		}
	}

	public static void unregisterSensorListener(Context context, SensorEventListener listener) {
		((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).unregisterListener(listener);
	}

	public static int convertToPosivite360Degree(int degree) {
		while (degree < 0) {
			degree += 360;
		}
		while (degree > 360) {
			degree -= 360;
		}
		return degree;
	}

	public static void updateCompass(final boolean force, final Location currentLocation, int orientation, final long now, final int scrollState,
			final long lastCompassChanged, final int lastCompassInDegree, final int minThresoldInMs, final SensorTaskCompleted callback) {
		if (currentLocation == null || orientation < 0) {
			callback.onSensorTaskCompleted(false, orientation, now);
			return;
		}
		if (!force) {
			if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
				callback.onSensorTaskCompleted(false, orientation, now);
				return;
			}
			final long diffInMs = now - lastCompassChanged;
			final boolean tooSoon = diffInMs <= Math.max(minThresoldInMs, COMPASS_UPDATE_THRESOLD_IN_MS);
			if (tooSoon) {
				callback.onSensorTaskCompleted(false, orientation, now);
				return;
			}
			final float diffInDegree = Math.abs(lastCompassInDegree - orientation);
			final boolean notDifferentEnough = diffInDegree <= COMPASS_DEGREE_UPDATE_THRESOLD;
			if (notDifferentEnough) {
				callback.onSensorTaskCompleted(false, orientation, now);
				return;
			}
		}
		callback.onSensorTaskCompleted(true, orientation, now);
	}

	public static float getLocationDeclination(Location location) {
		return new GeomagneticField((float) location.getLatitude(), (float) location.getLongitude(), (float) location.getAltitude(), location.getTime())
				.getDeclination();
	}

	public interface CompassListener {
		void updateCompass(float orientation, boolean force);
	}

	public interface SensorTaskCompleted {
		void onSensorTaskCompleted(boolean result, int orientation, long now);
	}

}

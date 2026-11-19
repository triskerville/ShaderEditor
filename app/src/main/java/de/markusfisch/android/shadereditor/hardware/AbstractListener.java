package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import de.markusfisch.android.shadereditor.app.ShaderEditorApp;

public abstract class AbstractListener implements SensorEventListener {
	long last = 0;

	private final SensorManager sensorManager;

	private boolean listening = false;
	private Sensor sensor;

	AbstractListener(Context context) {
		sensorManager = (SensorManager) context.getSystemService(
				Context.SENSOR_SERVICE);
	}

	public void unregister() {
		if (sensor == null || !listening) {
			return;
		}

		sensorManager.unregisterListener(this);
		listening = false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		last = event.timestamp;
	}

	boolean register(int type) {
		if (listening) {
			// Already running, e.g. the accelerometer feeding both the
			// gravity and the linear acceleration fallback.
			return true;
		}
		if (sensorManager == null || (sensor == null &&
				(sensor = sensorManager.getDefaultSensor(type)) == null)) {
			return false;
		}

		last = 0;
		int delay = ShaderEditorApp.preferences.getSensorDelay();
		try {
			listening = sensorManager.registerListener(this, sensor, delay);
		} catch (SecurityException e) {
			// Android 12+ throws for rates above 200 Hz without
			// HIGH_SAMPLING_RATE_SENSORS, e.g. when the permission is
			// disabled by policy. Fall back to the fastest allowed rate.
			listening = sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_GAME);
		}

		return listening;
	}
}

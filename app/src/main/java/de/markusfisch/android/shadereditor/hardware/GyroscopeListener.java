package de.markusfisch.android.shadereditor.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;

/**
 * Integrates the gyroscope's rate of rotation (rad/s) around the device's
 * x, y and z axes over time, so {@link #rotation} holds the angle in
 * radians the device has turned around each axis since the shader started.
 */
public class GyroscopeListener extends AbstractListener {
	public final float[] rotation = new float[]{0, 0, 0};

	private static final float NS2S = 1f / 1000000000f;
	// Skip integration across gaps (e.g. sensor batching after the
	// screen was off) so a single stale rate isn't applied for seconds.
	private static final float MAX_DT = .5f;

	public GyroscopeListener(Context context) {
		super(context);
	}

	public boolean register() {
		rotation[0] = rotation[1] = rotation[2] = 0f;
		return register(Sensor.TYPE_GYROSCOPE);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (last > 0) {
			final float dT = (event.timestamp - last) * NS2S;
			if (dT > 0 && dT < MAX_DT) {
				rotation[0] += event.values[0] * dT;
				rotation[1] += event.values[1] * dT;
				rotation[2] += event.values[2] * dT;
			}
		}

		last = event.timestamp;
	}
}
